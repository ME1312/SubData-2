package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.Protocol.*;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;

import static net.ME1312.SubData.Client.Library.ConnectionState.*;

/**
 * Initial Packet for Changing States Class
 */
public final class InitPacketChangeState implements InitialProtocol.Packet, PacketIn, PacketOut {

    public InitPacketChangeState() {}

    @Override
    public void sending(SubDataSender sender) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), sender.getConnection()) == INITIALIZATION) {
            Util.reflect(SubDataClient.class.getDeclaredField("isdcr"), sender.getConnection(), null);
            Util.reflect(SubDataClient.class.getDeclaredField("state"), sender.getConnection(), POST_INITIALIZATION);
        } else if (Util.reflect(SubDataClient.class.getDeclaredField("state"), sender.getConnection()) == POST_INITIALIZATION) {
            setReady(sender.getConnection());
        }
    }

    @Override
    public void receive(SubDataSender sender) throws Throwable {
        sender.sendPacket(new InitPacketChangeState());
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
