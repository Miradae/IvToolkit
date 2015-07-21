/*
 * Copyright 2014 Lukas Tenbrink
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ivorius.ivtoolkit.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Created by lukas on 01.07.14.
 */
public class PacketGuiAction implements IMessage
{
    private String context;
    private ByteBuf payload;

    public PacketGuiAction()
    {
    }

    public PacketGuiAction(String context, ByteBuf payload)
    {
        this.context = context;
        this.payload = payload;
    }

    public static PacketGuiAction packetGuiAction(String context, Number... args)
    {
        ByteBuf payload = Unpooled.buffer();

        for (Number num : args)
        {
            IvPacketHelper.writeNumber(payload, num);
        }

        return new PacketGuiAction(context, payload);
    }

    public String getContext()
    {
        return context;
    }

    public void setContext(String context)
    {
        this.context = context;
    }

    public ByteBuf getPayload()
    {
        return payload;
    }

    public void setPayload(ByteBuf payload)
    {
        this.payload = payload;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        context = ByteBufUtils.readUTF8String(buf);
        payload = IvPacketHelper.readByteBuffer(buf);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        ByteBufUtils.writeUTF8String(buf, context);
        IvPacketHelper.writeByteBuffer(buf, payload);
    }

    public static interface ActionHandler
    {
        void handleAction(String context, ByteBuf buffer);
    }
}
