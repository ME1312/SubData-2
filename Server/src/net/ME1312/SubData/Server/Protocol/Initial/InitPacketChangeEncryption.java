package net.ME1312.SubData.Server.Protocol.Initial;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.*;
import net.ME1312.SubData.Server.Library.*;
import net.ME1312.SubData.Server.Library.Exception.EncryptionException;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;

import java.util.HashMap;

/**
 * Initial Packet for Changing Encryption Class
 */
public final class InitPacketChangeEncryption implements InitialProtocol.Packet, PacketIn, PacketObjectOut<Integer> {

    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.INITIALIZATION) {
            Util.reflect(SubDataClient.class.getDeclaredField("isdcr"), client, DisconnectReason.ENCRYPTION_MISMATCH);
            ObjectMap<Integer> data = new ObjectMap<Integer>();

            String cipher = Util.reflect(SubDataServer.class.getDeclaredField("cipher"), client.getServer());
            String[] ciphers = (cipher.contains("/"))?cipher.split("/"):new String[]{cipher};
            Cipher last = Util.reflect(SubDataClient.class.getDeclaredField("cipher"), client);
            Pair<Cipher, String> next;
            int i = Util.reflect(SubDataClient.class.getDeclaredField("cipherlevel"), client);

            if (i <= 0) {
                next = new ContainedPair<>(Util.<HashMap<String, Cipher>>reflect(SubDataProtocol.class.getDeclaredField("ciphers"), client.getServer().getProtocol()).get(ciphers[0].toUpperCase()), null);
            } else if (last instanceof CipherFactory) {
                next = ((CipherFactory) last).newCipher(ciphers[i].toUpperCase());
            } else {
                next = null;
            }

            if (next != null && next.key() != null) {
                if (next.key() != last) last.retire(client);
                data.set(0x0000, next.key().getName());
                if (next.value() != null) data.set(0x0001, next.value());
                Util.reflect(SubDataClient.class.getDeclaredField("cipher"), client, next.key());
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
            OutputStreamL1 out = Util.reflect(SubDataClient.class.getDeclaredField("out"), client);
            out.control('\u0018');
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
