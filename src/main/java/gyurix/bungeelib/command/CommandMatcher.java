package gyurix.bungeelib.command;

import com.google.gson.internal.Primitives;
import gyurix.bungeelib.utils.BU;
import gyurix.bungeelib.utils.Reflection;
import gyurix.bungeelib.utils.StringUtils;
import lombok.Getter;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.lang.reflect.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static gyurix.bungeelib.BungeeLib.lang;
import static gyurix.bungeelib.utils.BU.bl;

public class CommandMatcher {
    private static HashMap<Class, CustomMatcher> customMatchers = new HashMap<>();
    @Getter
    private boolean async;
    private Method executor;
    private Object executorOwner;
    private Parameter[] parameters;
    private String pluginName;
    private Class senderType;

    public CommandMatcher(String pluginName, Object executorOwner, Method executor) {
        this.pluginName = pluginName;
        this.executorOwner = executorOwner;
        this.executor = executor;
        Async async = executorOwner.getClass().getAnnotation(Async.class);
        if (async != null && async.async())
            this.async = true;

        Parameter[] pars = executor.getParameters();
        for (Parameter p : pars) {
            Async as = p.getAnnotation(Async.class);
            if (as != null) {
                this.async = as.async();
                break;
            }
            as = p.getType().getAnnotation(Async.class);
            if (as != null && as.async()) {
                this.async = true;
                break;
            }
        }
        this.senderType = pars[0].getType();
        this.parameters = new Parameter[pars.length - 1];
        System.arraycopy(pars, 1, parameters, 0, parameters.length);
    }

    public static void addCustomMatcher(CustomMatcher m, Class... classes) {
        for (Class cl : classes)
            customMatchers.put(cl, m);
    }

    public static void registerCustomMatchers() {
        addCustomMatcher((arg, type) -> {
            try {
                return BU.bc.getPlayer(UUID.fromString(arg));
            } catch (Throwable ignored) {
            }
            return BU.bc.getPlayer(arg);
        }, UserConnection.class, ProxiedPlayer.class);
        addCustomMatcher((arg, type) -> BU.bc.getServerInfo(arg), ServerInfo.class);
        addCustomMatcher((arg, type) -> arg.equalsIgnoreCase("on") ||
                arg.equalsIgnoreCase("true") ||
                arg.equalsIgnoreCase("yes") ||
                arg.equalsIgnoreCase("y") ||
                arg.equalsIgnoreCase(""), boolean.class, Boolean.class);
    }

    public static void removeCustomMatcher(Class... classes) {
        for (Class cl : classes)
            customMatchers.remove(cl);
    }

    public Object convert(String arg, Type type) {
        Class cl = (Class) (type instanceof ParameterizedType ? ((ParameterizedType) type).getRawType() : type);
        cl = Primitives.wrap(cl);
        if (cl == String.class)
            return arg;
        CustomMatcher cm = customMatchers.get(cl);
        if (cm != null)
            return cm.convert(arg, type);
        if (Collection.class.isAssignableFrom(cl)) {
            String[] d = arg.split(",");
            Type target = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
            Collection out = (Collection) Reflection.newInstance(cl);
            for (String subArg : d)
                out.add(convert(subArg, target));
            return out;
        } else if (cl.isArray()) {
            String[] d = arg.split(",");
            int len = d.length;
            Class target = cl.getComponentType();
            Object out = Array.newInstance(target, len);
            for (int i = 0; i < len; ++i)
                Array.set(out, i, convert(d[i], target));
            return out;
        } else if (Map.class.isAssignableFrom(cl)) {
            String[] d = arg.split(",");
            Type targetKey = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
            Type targetValue = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
            Map out = (Map) Reflection.newInstance(cl);
            for (String subArg : d) {
                String[] d2 = subArg.split("=", 2);
                out.put(convert(d2[0], targetKey), convert(d2[1], targetValue));
            }
            return out;
        }
        try {
            return cl.getConstructor(String.class).newInstance(arg);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable e) {
            return null;
        }
        try {
            return cl.getMethod("valueOf", String.class).invoke(null, arg);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable e) {
            return null;
        }
        try {
            return cl.getMethod("fromString", String.class).invoke(null, arg);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable e) {
            return null;
        }
        try {
            return cl.getMethod("of", String.class).invoke(null, arg);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable e) {
            return null;
        }
        return null;
    }

    public void execute(CommandSender sender, String[] args) {
        if (!senderType.isAssignableFrom(sender.getClass())) {
            lang.msg("", sender, "command.noconsole");
            return;
        }
        Object[] out = new Object[parameters.length + 1];
        out[0] = sender;
        System.arraycopy(args, 0, out, 1, parameters.length);
        for (int i = 0; i < parameters.length; ++i) {
            Equals eq = parameters[i].getAnnotation(Equals.class);
            if (eq != null && !(eq.ignoreCase() ? args[i].equalsIgnoreCase(eq.value()) : args[i].equals(eq.value()))) {
                lang.msg("", sender, "command.wrongarg", "type", getParameterName(parameters[i]), "value", args[i]);
                return;
            }
            Object res = convert(args[i], parameters[i].getParameterizedType());
            if (res == null) {
                lang.msg("", sender, "command.wrongarg", "type", getParameterName(parameters[i]), "value", args[i]);
                return;
            }
            ArgRange as = parameters[i].getAnnotation(ArgRange.class);
            if (as != null) {
                Comparable min = (Comparable) convert(as.min(), parameters[i].getParameterizedType());
                Comparable max = (Comparable) convert(as.min(), parameters[i].getParameterizedType());
                if (min.compareTo(res) > 0) {
                    lang.msg("", sender, "command.toolow", "type", getParameterName(parameters[i]), "value", min);
                    return;
                }
                if (max.compareTo(res) < 0) {
                    lang.msg("", sender, "command.toohigh", "type", getParameterName(parameters[i]), "value", max);
                    return;
                }
            }
            out[i + 1] = res;
        }
        try {
            executor.invoke(executorOwner, out);
        } catch (Throwable e) {
            BU.log(bl, executorOwner, StringUtils.join(out, ", "));
            for (int i = 0; i < out.length; ++i) {
                BU.log(bl, "should", executor.getParameterTypes()[i]);
                BU.log(bl, "is", i, out[i].getClass().getSimpleName());
            }
            BU.error(sender, e.getCause() == null ? e : e.getCause(), pluginName, "gyurix");
        }
    }

    public String[] getAliases() {
        Aliases al = executor.getAnnotation(Aliases.class);
        return al == null ? null : al.value();
    }

    public int getParameterCount() {
        return parameters.length;
    }

    public String getParameterName(Parameter p) {
        Arg a = p.getAnnotation(Arg.class);
        if (a != null)
            return a.value();
        a = p.getType().getAnnotation(Arg.class);
        if (a != null)
            return a.value();
        return p.getType().getSimpleName().toLowerCase();
    }

    public String getUsage(String[] args) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Parameter p : parameters) {
            if (args.length == i)
                sb.append(" §6§l<");
            else if (args.length < i)
                sb.append(" §e<");
            else if (!isValidParameter(i, args[i]))
                sb.append(" §c<");
            else
                sb.append(" §a<");
            sb.append(getParameterName(p)).append('>');
            ++i;
        }
        return sb.substring(1);
    }

    public boolean isValidParameter(int id, String value) {
        Equals eq = parameters[id].getAnnotation(Equals.class);
        if (eq != null)
            return eq.ignoreCase() ? value.equalsIgnoreCase(eq.value()) : value.equals(eq.value());
        Object res = convert(value, parameters[id].getParameterizedType());
        if (res == null)
            return false;
        ArgRange as = parameters[id].getAnnotation(ArgRange.class);
        if (as != null) {
            Comparable min = (Comparable) convert(as.min(), parameters[id].getParameterizedType());
            Comparable max = (Comparable) convert(as.min(), parameters[id].getParameterizedType());
            return min.compareTo(res) <= 0 && max.compareTo(res) >= 0;
        }
        return true;
    }

    public boolean senderMatch(CommandSender sender) {
        return senderType.isAssignableFrom(sender.getClass());
    }
}