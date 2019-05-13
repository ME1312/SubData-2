package net.ME1312.SubData.Server.Protocol.Initial;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Library.ConnectionState;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.util.HashMap;
import java.util.LinkedList;

import static net.ME1312.SubData.Server.Library.ConnectionState.*;

/**
 * Initial Packet for Changing States Class
 */
public final class InitPacketChangeState implements InitialProtocol.Packet, InitialPacket, PacketIn, PacketOut {

    @Override
    public void receive(SubDataClient client) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == INITIALIZATION) {
            HashMap<ConnectionState, LinkedList<PacketOut>> queue = Util.reflect(SubDataClient.class.getDeclaredField("statequeue"), client);

            Util.reflect(SubDataClient.class.getDeclaredField("state"), client, POST_INITIALIZATION);
            client.sendPacket(new InitPacketVerifyState());
        } else if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == POST_INITIALIZATION) {
            setReady(client, true);
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}