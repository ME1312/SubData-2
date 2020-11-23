package net.ME1312.SubData.Server.Protocol.Initial;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Library.ConnectionState;
import net.ME1312.SubData.Server.Library.DisconnectReason;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;

import static net.ME1312.SubData.Server.Library.ConnectionState.*;

/**
 * Initial Packet for Changing States Class
 */
public final class InitPacketChangeState implements InitialProtocol.Packet, PacketIn, PacketOut {

    @Override
    public void sending(SubDataClient client) throws Throwable {
        Util.reflect(SubDataClient.class.getDeclaredField("isdcr"), client, null);
    }

    @Override
    public void receive(SubDataClient client) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == INITIALIZATION && (client.getServer().getProtocol().getAuthService() == null || client.getAuthResponse() != null)) {
            HashMap<ConnectionState, LinkedList<PacketOut>> queue = Util.reflect(SubDataClient.class.getDeclaredField("statequeue"), client);

            Util.reflect(SubDataClient.class.getDeclaredField("state"), client, POST_INITIALIZATION);
            Util.<Timer>reflect(SubDataClient.class.getDeclaredField("timeout"), client).cancel();
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
