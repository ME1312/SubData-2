package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Config.YAMLSection;

/**
 * Serializable Client Handler Layout Class
 */
public interface SerializableClientHandler extends ClientHandler {

    /**
     * Serialize this object for SubData
     *
     * @return Serialized Object
     */
    YAMLSection forSubData();
}
