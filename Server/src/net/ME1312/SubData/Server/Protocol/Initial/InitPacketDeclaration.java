package net.ME1312.SubData.Server.Protocol.Initial;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Library.ConnectionState;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;

/**
 * Initial Packet for Declaration Class
 */
public final class InitPacketDeclaration implements InitialPacket, PacketIn, PacketObjectOut {
    @Override
    public YAMLSection send(SubDataClient client) throws Throwable {
        YAMLSection data = new YAMLSection();
        data.set("n", client.getServer().getProtocol().getName());
        data.set("v", client.getServer().getProtocol().getVersion());
        data.set("id", client.getID());
        return data;
    }

    @Override
    public void receive(SubDataClient client) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.PRE_INITIALIZATION) {
            Util.reflect(SubDataClient.class.getDeclaredField("state"), client, ConnectionState.INITIALIZATION);
            client.sendPacket(new InitPacketChangeEncryption());
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
