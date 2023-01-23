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
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.database.IdentityManager;
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
import io.netty.channel.ChannelHandlerContext;

public class OmnicommProtocolDecoder extends BaseProtocolDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(OmnicommProtocolDecoder.class);

    public OmnicommProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static IdentityManager identityManager = Context.getIdentityManager();

    public static final int MSG_IDENTIFICATION = 0x80;
    public static final int MSG_ARCHIVE_INQUIRY = 0x85;
    public static final int MSG_ARCHIVE_DATA = 0x86;
    public static final int MSG_REMOVE_ARCHIVE_INQUIRY = 0x87;
    public static final int MSG_REMOVE_ARCHIVE_CONFIRMATION = 0x88;
    public static final int MSG_TERMINAL_KEEP_SESSION = 0x95;

    // #region Message Codes
    private static final int MSG_MID_TIMER = 0x01;
    private static final int MSG_MID_DRIVER_ASSIGNMENT = 0x02;
    private static final int MSG_MID_IGNITION_OF_POWER_SUPP_DISCONNECTION = 0x03;
    private static final int MSG_MID_IGNITION_TURNING_ON = 0x04;
    private static final int MSG_MID_IGN_TURNING_OFF = 0x05;
    private static final int MSG_MID_SWITCH_TO_BATTERY = 0x06;
    private static final int MSG_MID_SWITCH_TO_POWER_SUPPLY = 0x07;
    private static final int MSG_MID_PANIC_BUT_PRESSING = 0x08;
    private static final int MSG_TURNING_POINT = 0x09;
    private static final int MSG_MID_BAT_BACKUP_CATEGORY = 0x0A;
    private static final int MSG_MID_DEVICE_OPENING = 0x0B;
    private static final int MSG_MID_COMPLETE_POW_SUP_DISCONNECT = 0x0C;
    private static final int MSG_MID_VOICE_CALL = 0x0D;
    private static final int MSG_MID_SMS_MESSAGE = 0x0E;
    private static final int MSG_MID_PHOTO_CAM_RECEIVED = 0x0F;
    private static final int MSG_MID_SMOOTH_TURN = 0x10;
    private static final int MSG_MID_TERMINAL_SET_CHANGE = 0x11;
    private static final int MSG_MID_LOGS_RECORDING = 0x12;
    private static final int MSG_MID_ATTEMPY_CONN_IN_SLEEP = 0x13;
    // #endregion

    public static final HashMap<Integer, String> MSG_MID_DESCRIPTIONS = new HashMap<Integer, String>();

    static {
        MSG_MID_DESCRIPTIONS.put(MSG_MID_TIMER, "timer");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_DRIVER_ASSIGNMENT, "driverAssignment");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_IGNITION_OF_POWER_SUPP_DISCONNECTION, "ignitionOfPowerSupplyDisconnection");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_IGN_TURNING_OFF, "ignitionTurningOff");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_IGNITION_TURNING_ON, "ignitionTurningOn");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_SWITCH_TO_BATTERY, "switchingToBattery");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_SWITCH_TO_POWER_SUPPLY, "switchingToPrimaryPowerSupply");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_PANIC_BUT_PRESSING, "panicButtonPressing");
        MSG_MID_DESCRIPTIONS.put(MSG_TURNING_POINT, "turningPoint");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_BAT_BACKUP_CATEGORY, "batteryBackupCategory");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_DEVICE_OPENING, "deviceOpening");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_COMPLETE_POW_SUP_DISCONNECT, "completePowerSupplyDisconnection");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_VOICE_CALL, "voiceCall");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_SMS_MESSAGE, "smsMessage");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_PHOTO_CAM_RECEIVED, "photoFromCameraReceived");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_SMOOTH_TURN, "smoothTurningOnTheRoad");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_TERMINAL_SET_CHANGE, "terminalSettingsChanged");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_LOGS_RECORDING, "logsRecording");
        MSG_MID_DESCRIPTIONS.put(MSG_MID_ATTEMPY_CONN_IN_SLEEP, "attemptToConnectInSleep");
    }

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
        Device device = null;

        buf.readUnsignedByte(); // prefix
        int commandType = buf.readUnsignedByte();
        int length = buf.readUnsignedShortLE();

        DeviceSession deviceSession;

        switch (commandType) {
            case MSG_IDENTIFICATION:
                long terminalID = buf.readUnsignedIntLE();
                deviceSession = getDeviceSession(channel, remoteAddress, String.valueOf(terminalID));

                if (deviceSession != null) {
                    device = getDeviceFromSession(deviceSession);
                    sendArchiveInquiryCommand(channel, device, deviceSession);
                }
                break;

            case MSG_ARCHIVE_DATA:
            case MSG_TERMINAL_KEEP_SESSION:
                deviceSession = getDeviceSession(channel, remoteAddress);
                if (deviceSession == null) {
                    return null;
                }

                device = getDeviceFromSession(deviceSession);
                device.set(Device.KEY_OMNICOMM_LAST_ARCHIVE_TIME, getFormattedNowTime());

                if (length == 0) {
                    device.set(Device.KEY_OMNICOMM_LAST_DELETE_TIME, getFormattedNowTime());
                    sendResponse(channel, MSG_REMOVE_ARCHIVE_INQUIRY, deviceSession.getRecordNumber());
                    return null;
                }

                long recordNumber = buf.readUnsignedIntLE();
                buf.readUnsignedIntLE(); // time
                buf.readUnsignedByte(); // priority

                List<Position> positions = new LinkedList<>();

                decodePositionsData(buf, deviceSession, positions);

                updateDeviceRecordNumber(device, deviceSession, recordNumber);

                if (!positions.isEmpty()) {
                    return positions;
                }

                break;
            case MSG_REMOVE_ARCHIVE_CONFIRMATION:
                LOGGER.warn("Clearing data in device");
                break;
            default:
                LOGGER.warn("We have no idea what this is. Received command: " + commandType);

                deviceSession = getDeviceSession(channel, remoteAddress);
                sendResponse(channel, MSG_ARCHIVE_INQUIRY, deviceSession.getRecordNumber());
                break;
        }

        return null;
    }

    private void decodePositionsData(ByteBuf buf, DeviceSession deviceSession, List<Position> positions)
            throws InvalidProtocolBufferException {
        while (buf.readableBytes() > 2) {

            OmnicommMessageOuterClass.OmnicommMessage message = parseProto(buf, buf.readUnsignedShortLE());

            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());

            if (message.hasGeneral()) {
                readGeneralParameters(message, position);
            }

            if (message.hasNAV()) {
                readNAVParameters(message, position);
            }

            if (message.hasLLSDt()) {
                readLLSData(message, position);
            }

            readMessageIDs(message, position);

            if (position.getFixTime() != null) {
                positions.add(position);
            }
        }
    }

    private void sendArchiveInquiryCommand(Channel channel, Device device, DeviceSession deviceSession) {
        try {
            if (device != null && device.getId() != 0) {
                device.set(Device.KEY_OMNICOMM_LAST_IDENTIFICATION_TIME,
                        getFormattedNowTime());

                long recordNumber = device.getLong(Device.KEY_OMNICOMM_RECORD_NUMBER);
                recordNumber = recordNumber == 0 ? 0 : getRandomRecordBetween(0, 20, recordNumber);
                deviceSession.setRecordNumber(recordNumber);

                sendResponse(channel, MSG_ARCHIVE_INQUIRY, deviceSession.getRecordNumber());

            }
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
    }

    private long getRandomRecordBetween(int min, int max, long currentRecordNumber) {
        if (currentRecordNumber > max) {
            return currentRecordNumber - Math.round(Math.random() * (max - min + 1) + max);
        }

        return currentRecordNumber + 1;

    }

    private Device getDeviceFromSession(DeviceSession deviceSession) {
        Device device;
        device = identityManager.getById(deviceSession.getDeviceId());
        return device;
    }

    private String getFormattedNowTime() {
        return DateFormat.getDateTimeInstance().format(new Date());
    }

    private void updateDeviceRecordNumber(Device device, DeviceSession deviceSession, long recordNumber) {
        if (device != null) {
            device.set(Device.KEY_OMNICOMM_ATTEMPTS_TIMER, 0); // reset attempts timer
            device.set(Device.KEY_OMNICOMM_RECORD_NUMBER, recordNumber);
            deviceSession.setRecordNumber(recordNumber);
        }
    }

    private Date getUnixTime(int omnicommTime) {
        return new Date((omnicommTime + 1230768000) * 1000L); // from 2009-01-01 12:00
    }

    /**
     * @param message
     * @param position
     */
    private void readLLSData(OmnicommMessageOuterClass.OmnicommMessage message, Position position) {
        if (message.hasLLSDt()) {
            OmnicommMessageOuterClass.OmnicommMessage.LLSDt data = message.getLLSDt();
            position.set(Position.KEY_HAS_FUEL_DATA, true);

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
        }
    }

    private void readGeneralParameters(OmnicommMessageOuterClass.OmnicommMessage message, Position position) {
        OmnicommMessageOuterClass.OmnicommMessage.General data = message.getGeneral();
        position.setTime(getUnixTime(data.getTime()));

        position.set(Position.KEY_POWER, data.getUboard() * 0.1);
        position.set(Position.KEY_BATTERY_LEVEL, data.getBatLife());

        int flg = data.getFLG();
        position.set(Position.KEY_IGNITION, BitUtil.check(flg, 0));
        position.set(Position.KEY_GSM_CONNECTION_AVAILABLE, BitUtil.check(flg, 1));
        position.set(Position.KEY_GPS_DATA_ACCURATE, BitUtil.check(flg, 2));
        position.set(Position.KEY_ROAMING, BitUtil.check(flg, 3));
        position.set(Position.KEY_RESERVE_POWER, BitUtil.check(flg, 4));
        position.set(Position.KEY_PANIC_BUTTON_PRESSED, BitUtil.check(flg, 5));
        position.set(Position.KEY_DEVICE_OPENING, BitUtil.check(flg, 6));
        position.set(Position.KEY_DISCRETE_OUTPUT_ENABLED, BitUtil.check(flg, 7));

        position.set(Position.KEY_RPM, data.getTImp());
        position.set(Position.KEY_DISTANCE, data.getMileage() * 0.1);
    }

    private void readNAVParameters(OmnicommMessageOuterClass.OmnicommMessage message, Position position) {
        if (message.hasNAV()) {
            OmnicommMessageOuterClass.OmnicommMessage.NAV data = message.getNAV();
            position.setValid(true);

            int gpsTime = data.getGPSTime();
            if (gpsTime > 0 || position.getDeviceTime() == null) {
                position.setTime(getUnixTime(gpsTime));
            }

            position.setLatitude(data.getLAT() * 0.0000001);
            position.setLongitude(data.getLON() * 0.0000001);
            position.setSpeed(UnitsConverter.knotsFromKph(data.getGPSVel() * 0.1));
            position.setCourse(data.getGPSDir());
            position.setAltitude(data.getGPSAlt() * 0.1);
            position.set(Position.KEY_SATELLITES, data.getGPSNSat());
        } else {
            position.setValid(false);
        }
    }

    private void readMessageIDs(OmnicommMessageOuterClass.OmnicommMessage message, Position position) {
        position.setValid(message.getMIDList().contains(MSG_MID_TIMER));
        List<String> messageIDs = message.getMIDList().stream().map(mid -> MSG_MID_DESCRIPTIONS.get(mid))
                .collect(Collectors.toList());
        position.set(Position.KEY_TYPE, String.join(",", messageIDs));
    }

    private void setLLSData(int number, int tllsValue, int cllsValue, int fllsValue, Position position) {
        String parameterName = "fuel" + number;
        position.set(parameterName + "Temp", tllsValue);
        position.set(parameterName, cllsValue);
        position.set(parameterName + "State", fllsValue);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error(getProtocolName(), cause);

        super.exceptionCaught(ctx, cause);
    }

}
