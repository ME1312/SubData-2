package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Cipher;
import net.ME1312.SubData.Client.CipherFactory;
import net.ME1312.SubData.Client.Library.ConnectionState;
import net.ME1312.SubData.Client.Library.DebugUtil;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.Library.Exception.EncryptionException;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataProtocol;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Initial Packet for Changing Encryption Class
 */
public final class InitPacketChangeEncryption implements InitialPacket, PacketObjectIn, PacketOut {
    static HashMap<SubDataClient, Integer> levels = new HashMap<SubDataClient, Integer>();

    @Override
    public void receive(SubDataClient client, YAMLSection data) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.INITIALIZATION) {
            ArrayList<SubDataClient> tmp = new ArrayList<SubDataClient>();
            tmp.addAll(levels.keySet());
            for (SubDataClient next : tmp) if (next.isClosed()) levels.remove(next);
            if (!levels.keySet().contains(client))
                levels.put(client, 0);

            Cipher last = Util.reflect(SubDataClient.class.getDeclaredField("cipher"), client);
            Cipher next;
            int i = levels.get(client);

            if (i <= 0) {
                next = Util.<HashMap<String, Cipher>>reflect(SubDataProtocol.class.getDeclaredField("ciphers"), client.getProtocol()).get(data.getRawString("e").toUpperCase());
            } else if (last instanceof CipherFactory) {
                next = ((CipherFactory) last).getCipher(data.getRawString("e").toUpperCase(), (data.contains("k"))?data.getRawString("k"):null);
            } else {
                next = null;
            }

            if (next != null) {
                Util.reflect(SubDataClient.class.getDeclaredField("cipher"), client, next);
                levels.put(client, levels.get(client) + 1);

                client.getSocket().getOutputStream().write('\u0018');
                client.getSocket().getOutputStream().flush();
                client.sendPacket(this);
            } else {
                DebugUtil.logException(new EncryptionException("Unknown encryption type \"" + data.getRawString("e") + '\"' + ((i <= 0)?"":" in \"" + last + '\"')), Util.reflect(SubDataProtocol.class.getDeclaredField("log"), client.getProtocol()));
                Util.reflect(SubDataClient.class.getDeclaredMethod("close", DisconnectReason.class), client, DisconnectReason.ENCRYPTION_MISMATCH);
            }
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
