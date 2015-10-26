package com.app2.proxy.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.ByteArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;

// This class is a convenient place to keep things common to both the client and server.
public class Network {

    // This registers objects that are going to be sent over the network.
    static public void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(RegisterName.class);
        kryo.register(String[].class);
        kryo.register(UpdateNames.class);
        kryo.register(ChatMessage.class);

        FieldSerializer<Data> dataClassSerializer = new FieldSerializer<Data>(kryo, Data.class);
        dataClassSerializer.getField("bytes").setClass(byte[].class, new ByteArraySerializer());
        dataClassSerializer.getField("string").setClass(String.class, new StringSerializer());
        dataClassSerializer.getField("string").setCanBeNull(true);
        kryo.register(Data.class, dataClassSerializer);
    }

    static public class RegisterName {
        public String name;
    }

    static public class UpdateNames {
        public String[] names;
    }

    static public class ChatMessage {
        public String text;
    }
}
