package net.ME1312.SubData.Server.Protocol.Initial;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.*;
import net.ME1312.SubData.Server.Library.ConnectionState;
import net.ME1312.SubData.Server.Library.DebugUtil;
import net.ME1312.SubData.Server.Library.DisconnectReason;
import net.ME1312.SubData.Server.Library.Exception.EncryptionException;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;

import java.util.HashMap;

/**
 * Initial Packet for Changing Encryption Class
 */
public final class InitPacketChangeEncryption implements InitialPacket, PacketIn, PacketObjectOut<Integer> {

    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.INITIALIZATION) {
            ObjectMap<Integer> data = new ObjectMap<Integer>();

            String cipher = Util.reflect(SubDataServer.class.getDeclaredField("cipher"), client.getServer());
            String[] ciphers = (cipher.contains("/"))?cipher.split("/"):new String[]{cipher};
            Cipher last = Util.reflect(SubDataClient.class.getDeclaredField("cipher"), client);
            NamedContainer<Cipher, String> next;
            int i = Util.reflect(SubDataClient.class.getDeclaredField("cipherlevel"), client);

            if (i <= 0) {
                next = new NamedContainer<>(Util.<HashMap<String, Cipher>>reflect(SubDataProtocol.class.getDeclaredField("ciphers"), client.getServer().getProtocol()).get(ciphers[0].toUpperCase()), null);
            } else if (last instanceof CipherFactory) {
                next = ((CipherFactory) last).newCipher(ciphers[i].toUpperCase());
            } else {
                next = null;
            }

            if (next != null && next.name() != null) {
                data.set(0x0000, next.name().getName());
                if (next.get() != null) data.set(0x0001, next.get());
                Util.reflect(SubDataClient.class.getDeclaredField("cipher"), client, next.name());
                return data;
            } else {
                DebugUtil.logException(new EncryptionException("Unknown encryption type \"" + ciphers[i] + '\"' + ((i <= 0)?"":" in \"" + last + '\"')), Util.reflect(SubDataServer.class.getDeclaredField("log"), client.getServer()));
                Util.reflect(SubDataClient.class.getDeclaredMethod("close", DisconnectReason.class), client, DisconnectReason.ENCRYPTION_MISMATCH);
                return null;
            }
        } else return null;
    }

    @Override
    public void receive(SubDataClient client) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.INITIALIZATION) {
            int level = Util.<Integer>reflect(SubDataClient.class.getDeclaredField("cipherlevel"), client) + 1;
            Util.reflect(SubDataClient.class.getDeclaredField("cipherlevel"), client, level);
            client.getSocket().getOutputStream().write('\u0018');
            client.getSocket().getOutputStream().flush();
            String cipher = Util.reflect(SubDataServer.class.getDeclaredField("cipher"), client.getServer());
            if (level < ((cipher.contains("/"))?cipher.split("/"):new String[]{cipher}).length) {
                client.sendPacket(this);
            } else {
                client.sendPacket(new InitPacketPostDeclaration());
            }
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
