package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.*;
import net.ME1312.SubData.Client.Library.*;
import net.ME1312.SubData.Client.Library.Exception.EncryptionException;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;

import java.util.HashMap;

/**
 * Initial Packet for Changing Encryption Class
 */
public final class InitPacketChangeEncryption implements InitialProtocol.Packet, PacketObjectIn<Integer>, PacketOut {

    @Override
    public void receive(SubDataSender sender, ObjectMap<Integer> data) throws Throwable {
        String cipher = data.getRawString(0x0000).toUpperCase();
        String key =       (data.contains(0x0001))?data.getRawString(0x0001):null;

        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), sender.getConnection()) == ConnectionState.INITIALIZATION) {
            Util.reflect(SubDataClient.class.getDeclaredField("isdcr"), sender.getConnection(), DisconnectReason.ENCRYPTION_MISMATCH);
            Cipher last = Util.reflect(SubDataClient.class.getDeclaredField("cipher"), sender.getConnection());
            Cipher next;
            int i = Util.reflect(SubDataClient.class.getDeclaredField("cipherlevel"), sender.getConnection());

            if (i <= 0) {
                next = Util.<HashMap<String, Cipher>>reflect(SubDataProtocol.class.getDeclaredField("ciphers"), sender.getProtocol()).get(cipher);
            } else if (last instanceof CipherFactory) {
                next = ((CipherFactory) last).getCipher(cipher, key);
            } else {
                next = null;
            }

            if (next != null) {
                if (next != last) last.retire(sender.getConnection());
                Util.reflect(SubDataClient.class.getDeclaredField("cipher"), sender.getConnection(), next);
                Util.reflect(SubDataClient.class.getDeclaredField("cipherlevel"), sender.getConnection(), Util.<Integer>reflect(SubDataClient.class.getDeclaredField("cipherlevel"), sender.getConnection()) + 1);

                OutputStreamL1 out = Util.reflect(SubDataClient.class.getDeclaredField("out"), sender.getConnection());
                out.control('\u0018');
                out.flush();
                sender.sendPacket(this);
            } else {
                DebugUtil.logException(new EncryptionException("Unknown encryption type \"" + cipher + '\"' + ((i <= 0)?"":" in \"" + last + '\"')), Util.reflect(SubDataClient.class.getDeclaredField("log"), sender.getConnection()));
                Util.reflect(SubDataClient.class.getDeclaredMethod("close", DisconnectReason.class), sender.getConnection(), DisconnectReason.ENCRYPTION_MISMATCH);
            }
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
