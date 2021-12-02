package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.ConnectionState;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;

/**
 * Initial Packet for Login Class
 */
public final class InitPacketLogin implements InitialProtocol.Packet, PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private ObjectMap<?> data;

    public InitPacketLogin() {}
    private InitPacketLogin(ObjectMap<?> data) {
        this.data = data;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender sender) throws Throwable {
        if (data == null) {
            return null;
        } else {
            ObjectMap<Integer> data = new ObjectMap<Integer>();
            data.set(0x0000, this.data);
            return data;
        }
    }

    @Override
    public void receive(SubDataSender sender, ObjectMap<Integer> data) throws Throwable {
        boolean required = data.getBoolean(0x0000);

        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), sender.getConnection()) == ConnectionState.INITIALIZATION) {
            Util.reflect(SubDataClient.class.getDeclaredField("isdcr"), sender.getConnection(), DisconnectReason.INVALID_LOGIN);
            if (required) {
                ObjectMap<?> login = Util.reflect(SubDataClient.class.getDeclaredField("login"), sender.getConnection());
                if (login != null) {
                    sender.sendPacket(new InitPacketLogin(login));
                } else {
                    sender.sendPacket(new InitPacketLogin());
                }
            } else {
                sender.sendPacket(new InitPacketLogin());
            }
        }
    }
}
