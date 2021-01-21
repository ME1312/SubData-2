package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.SubData.Client.Protocol.Internal.PacketDisconnect;
import net.ME1312.SubData.Client.Protocol.Internal.PacketDisconnectUnderstood;
import net.ME1312.SubData.Client.Protocol.Internal.PacketNull;
import net.ME1312.SubData.Client.Protocol.PacketIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;

import java.util.HashMap;

/**
 * Initial Protocol Value Class
 */
public final class InitialProtocol {
    private static final HashMap<Class<? extends PacketOut>, Integer> pOut = new HashMap<Class<? extends PacketOut>, Integer>();
    private static final HashMap<Integer, PacketIn> pIn = new HashMap<Integer, PacketIn>();
    private InitialProtocol() {}

    static {
        pIn.put(0x0000, new InitPacketDeclaration());
        pIn.put(0x0001, new InitPacketChangeEncryption());
        pIn.put(0x0002, new InitPacketPostDeclaration());
        pIn.put(0x0003, new InitPacketLogin());
        pIn.put(0xFFFA, new InitPacketVerifyState());
        pIn.put(0xFFFB, new InitPacketChangeState());
        pIn.put(0xFFFD, new PacketNull());
        pIn.put(0xFFFE, new PacketDisconnectUnderstood());
        pIn.put(0xFFFF, new PacketDisconnect());

        pOut.put(InitPacketDeclaration.class, 0x0000);
        pOut.put(InitPacketChangeEncryption.class, 0x0001);
        pOut.put(InitPacketPostDeclaration.class, 0x0002);
        pOut.put(InitPacketLogin.class, 0x0003);
        pOut.put(InitPacketVerifyState.class, 0xFFFA);
        pOut.put(InitPacketChangeState.class, 0xFFFB);
        pOut.put(PacketNull.class, 0xFFFD);
        pOut.put(PacketDisconnectUnderstood.class, 0xFFFE);
        pOut.put(PacketDisconnect.class, 0xFFFF);
    }

    /**
     * InitialProtocol Packet Tag Class<br>
     * Classes that implement this may be sent during the INITIALIZATION state
     */
    public interface Packet extends InitialPacket {
    }
}
