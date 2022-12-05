/*
 * Copyright 2019 - 2020 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.protobuf.omnicomm.OmnicommMessageOuterClass;

import com.google.protobuf.InvalidProtocolBufferException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

public class OmnicommProtocolDecoder extends BaseProtocolDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(OmnicommProtocolDecoder.class);

    public OmnicommProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_IDENTIFICATION = 0x80;
    public static final int MSG_ARCHIVE_INQUIRY = 0x85;
    public static final int MSG_ARCHIVE_DATA = 0x86;
    public static final int MSG_REMOVE_ARCHIVE_INQUIRY = 0x87;
    public static final int MSG_REMOVE_ARCHIVE_CONFIRMATION = 0x88;
    public static final int MSG_TERMINAL_KEEP_SESSION = 0x95;

    private OmnicommMessageOuterClass.OmnicommMessage parseProto(
            ByteBuf buf, int length) throws InvalidProtocolBufferException {

        final byte[] array;
        final int offset;
        if (buf.hasArray()) {
            array = buf.array();
            offset = buf.arrayOffset() + buf.readerIndex();
        } else {
            array = ByteBufUtil.getBytes(buf, buf.readerIndex(), length, false);
            offset = 0;
        }
        buf.skipBytes(length);

        return OmnicommMessageOuterClass.OmnicommMessage
                .getDefaultInstance().getParserForType().parseFrom(array, offset, length);
    }

    private void sendResponse(Channel channel, int commandType, long recordNumber) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(0xC0);
            response.writeByte(commandType);
            response.writeShortLE(4);
            response.writeIntLE((int) recordNumber);
            response.writeShortLE(Checksum.crc16(Checksum.CRC16_CCITT_FALSE,
                    response.nioBuffer(1, response.writerIndex() - 1)));
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        Device device;

        buf.readUnsignedByte(); // prefix
        int commandType = buf.readUnsignedByte();
        int length = buf.readUnsignedShortLE();

        if (commandType == MSG_IDENTIFICATION) {
            long terminalID = buf.readUnsignedIntLE();
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(terminalID));
            device = getIdentityManager().getById(deviceSession.getDeviceId());

            if (device != null && device.getId() != 0) {
                long recordNumber = device.getLong(Device.KEY_OMNICOMM_RECORD_NUMBER);
                deviceSession.setRecordNumber(recordNumber);
                sendResponse(channel, MSG_ARCHIVE_INQUIRY, deviceSession.getRecordNumber() + 1);
            }

        } else if (commandType == MSG_ARCHIVE_DATA || commandType == MSG_TERMINAL_KEEP_SESSION) {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            if (length == 0) {
                sendResponse(channel, MSG_REMOVE_ARCHIVE_INQUIRY, deviceSession.getRecordNumber());
                return null;
            }

            long recordNumber = buf.readUnsignedIntLE();
            buf.readUnsignedIntLE(); // time
            buf.readUnsignedByte(); // priority

            List<Position> positions = new LinkedList<>();

            while (buf.readableBytes() > 2) {

                OmnicommMessageOuterClass.OmnicommMessage message = parseProto(buf, buf.readUnsignedShortLE());

                Position position = new Position(getProtocolName());
                position.setDeviceId(deviceSession.getDeviceId());

                if (message.hasGeneral()) {
                    OmnicommMessageOuterClass.OmnicommMessage.General data = message.getGeneral();
                    position.set(Position.KEY_POWER, data.getUboard() * 0.1);
                    position.set(Position.KEY_BATTERY_LEVEL, data.getBatLife());
                    position.set(Position.KEY_IGNITION, BitUtil.check(data.getFLG(), 0));
                    position.set(Position.KEY_RPM, data.getTImp());
                }

                if (message.hasNAV()) {
                    OmnicommMessageOuterClass.OmnicommMessage.NAV data = message.getNAV();
                    position.setValid(true);
                    position.setTime(new Date((data.getGPSTime() + 1230768000) * 1000L)); // from 2009-01-01 12:00
                    position.setLatitude(data.getLAT() * 0.0000001);
                    position.setLongitude(data.getLON() * 0.0000001);
                    position.setSpeed(UnitsConverter.knotsFromKph(data.getGPSVel() * 0.1));
                    position.setCourse(data.getGPSDir());
                    position.setAltitude(data.getGPSAlt() * 0.1);
                    position.set(Position.KEY_SATELLITES, data.getGPSNSat());
                }

                if (message.hasLLSDt()) {
                    OmnicommMessageOuterClass.OmnicommMessage.LLSDt data = message.getLLSDt();

                    try {
                        if (data.hasTLLS1()) {
                            setLLSData(1, data.getTLLS1(), data.getCLLS1(), data.getFLLS1(), position);
                        }

                        if (data.hasTLLS2()) {
                            setLLSData(2, data.getTLLS2(), data.getCLLS2(), data.getFLLS2(), position);
                        }

                        if (data.hasTLLS3()) {
                            setLLSData(3, data.getTLLS3(), data.getCLLS3(), data.getFLLS3(), position);
                        }

                        if (data.hasTLLS4()) {
                            setLLSData(4, data.getTLLS4(), data.getCLLS4(), data.getFLLS4(), position);
                        }

                        if (data.hasTLLS5()) {
                            setLLSData(5, data.getTLLS5(), data.getCLLS5(), data.getFLLS5(), position);
                        }

                        if (data.hasTLLS6()) {
                            setLLSData(6, data.getTLLS6(), data.getCLLS6(), data.getFLLS6(), position);
                        }

                        if (data.hasTLLS7()) {
                            setLLSData(7, data.getTLLS7(), data.getCLLS7(), data.getFLLS7(), position);
                        }

                        if (data.hasTLLS8()) {
                            setLLSData(8, data.getTLLS8(), data.getCLLS8(), data.getFLLS8(), position);
                        }
                    } catch (Exception e) {
                        position.set("FUEL_PARAMETER_ERROR", e.getMessage());
                    }

                    /*
                     * position.set("fuel1Temp", data.getTLLS1());
                     * position.set("fuel1", data.getCLLS1());
                     * position.set("fuel1State", data.getFLLS1());
                     */
                }

                if (position.getFixTime() != null) {
                    positions.add(position);
                }
            }

            device = getIdentityManager().getById(deviceSession.getDeviceId());
            device.set(Device.KEY_OMNICOMM_RECORD_NUMBER, recordNumber);
            deviceSession.setRecordNumber(recordNumber);

            if (!positions.isEmpty()) {
                return positions;
            }
        } else if (commandType == MSG_REMOVE_ARCHIVE_CONFIRMATION) {
            LOGGER.warn("Clearing data in device");
        }

        return null;
    }

    private void setLLSData(int number, int tllsValue, int cllsValue, int fllsValue, Position position) {
        String parameterName = "fuel" + number;
        position.set(parameterName + "Temp", tllsValue);
        position.set(parameterName, cllsValue);
        position.set(parameterName + "State", fllsValue);
    }

}
