package gyurix.bungeelib.protocol.wrappers;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants.Direction;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
public class DeclareCommands extends DefinedPacket {
    private List<DeclareCommandNode> nodes = new ArrayList<>();
    private int rootIndex;

    public void read(ByteBuf buf, Direction direction, int protocolVersion) {
        int count = DefinedPacket.readVarInt(buf);
        for (int i = 0; i < count; ++i) {
            nodes.add(new DeclareCommandNode(buf));
        }
        this.rootIndex = DefinedPacket.readVarInt(buf);
    }

    public void write(ByteBuf buf, Direction direction, int protocolVersion) {
        int count = nodes.size();
        DefinedPacket.writeVarInt(count, buf);
        for (DeclareCommandNode n : nodes)
            n.write(buf);
        DefinedPacket.writeVarInt(rootIndex, buf);
    }

    public void handle(AbstractPacketHandler handler) {
        System.out.println("HANDLE DeclareCommands");
    }

    public String toString() {
        return "DeclareCommands(count=" + nodes.size() + ", rootIndex=" + rootIndex + ", nodes=" + nodes + ")";
    }

    public enum DeclaredCommandNodeType {
        ROOT, LITERAL, ARGUMENT, OTHER
    }

    @NoArgsConstructor
    @Data
    public static class DeclareCommandNode {
        private ArrayList<Integer> children = new ArrayList<>();
        private byte flags;
        private String name;
        private int redirectNode;

        public DeclareCommandNode(ByteBuf buf) {
            flags = buf.readByte();
            int childrenCount = DefinedPacket.readVarInt(buf);
            for (int i = 0; i < childrenCount; ++i)
                children.add(DefinedPacket.readVarInt(buf));
            if (hasRedirect())
                redirectNode = DefinedPacket.readVarInt(buf);
            DeclaredCommandNodeType type = getType();
            if (type == DeclaredCommandNodeType.ARGUMENT) {
                if (type == DeclaredCommandNodeType.LITERAL) {
                    name = DefinedPacket.readString(buf);
                }

            }
        }

        public DeclaredCommandNodeType getType() {
            return DeclaredCommandNodeType.values()[flags & 3];
        }

        public boolean hasRedirect() {
            return (flags & 8) == 8;
        }

        public boolean hasSuggestionsType() {
            return (flags & 16) == 16;
        }

        public boolean isExecutable() {
            return (flags & 4) == 4;
        }

        public void setExecutable(boolean value) {
            setFlag((byte) 4, value);
        }

        public void setFlag(byte id, boolean value) {
            flags = (byte) (value ? flags - (flags & id) + id : flags - (flags & id));
        }

        public void setRedirect(boolean value) {
            setFlag((byte) 8, value);
        }

        public void setSuggestionType(boolean value) {
            setFlag((byte) 16, value);
        }

        public void write(ByteBuf buf) {

        }
    }

    @NoArgsConstructor
    @Data
    public static class DeclareCommandNodeParser {

    }

    @NoArgsConstructor
    @Data
    public static class DeclaredCommandNodeIdentifier {

    }
}
