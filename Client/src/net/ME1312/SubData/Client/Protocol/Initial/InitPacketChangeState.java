package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.*;
import net.ME1312.SubData.Client.SubDataClient;

import static net.ME1312.SubData.Client.Library.ConnectionState.*;

/**
 * Initial Packet for Changing States Class
 */
public final class InitPacketChangeState implements InitialProtocol.Packet, InitialPacket, PacketIn, PacketOut {

    public InitPacketChangeState() {}

    @Override
    public void sending(SubDataClient client) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == INITIALIZATION) {
            Util.reflect(SubDataClient.class.getDeclaredField("state"), client, POST_INITIALIZATION);
        } else if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == POST_INITIALIZATION) {
            setReady(client, false);
        }
    }

    @Override
    public void receive(SubDataClient client) throws Throwable {
        client.sendPacket(new InitPacketChangeState());
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
