package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.SubData.Client.Protocol.Internal.PacketDisconnect;
import net.ME1312.SubData.Client.Protocol.Internal.PacketDisconnectUnderstood;
import net.ME1312.SubData.Client.Protocol.PacketIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;

import java.util.HashMap;

/**
 * Initial Protocol Container Class
 */
public final class InitialProtocol {
    private static final HashMap<Class<? extends PacketOut>, Integer> pOut = new HashMap<Class<? extends PacketOut>, Integer>();
    private static final HashMap<Integer, PacketIn> pIn = new HashMap<Integer, PacketIn>();
    private InitialProtocol() {}

    static {
        pIn.put(0x0000, new InitPacketDeclaration());
        pIn.put(0x0001, new InitPacketChangeEncryption());
        pIn.put(0x0002, new InitPacketPostDeclaration());
        pIn.put(0x0010, new InitPacketChangeProtocol());
        pIn.put(0xFFFE, new PacketDisconnectUnderstood());
        pIn.put(0xFFFF, new PacketDisconnect());

        pOut.put(InitPacketDeclaration.class, 0x0000);
        pOut.put(InitPacketChangeEncryption.class, 0x0001);
        pOut.put(InitPacketPostDeclaration.class, 0x0002);
        pOut.put(InitPacketChangeProtocol.class, 0x0010);
        pOut.put(PacketDisconnectUnderstood.class, 0xFFFE);
        pOut.put(PacketDisconnect.class, 0xFFFF);
    }
}
