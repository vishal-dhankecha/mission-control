package com.example.vishal_pc.receiver;


import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;


public class MSP {
    // TODO create classes for constant value for each msp version
    // we might have more than 8 motors in the future
    private int MAXSERVO = 8;
    private int MAXMOTOR = 8;

    public int UAV_TRI = 1;
    public int UAV_QUADP = 2;
    public int UAV_QUADX = 3;
    public int UAV_BI = 4;
    public int UAV_GUIMBAL = 5; // TODO
    public int UAV_Y6 = 6;

    // For accessing values that change over time (Jfreechart want a String)
    public String IDANGX = "angx";
    public String IDANGY = "angy";
    public String IDHEAD = "head";
    public String IDALT = "alt";

    public Double IDAX = 0.0;
    public Double IDAY = 0.0;
    public Double IDAZ = 0.0;
    public Double IDGX = 0.0;
    public Double IDGY = 0.0;
    public Double IDGZ = 0.0;

    public Double IDMAGX = 0.0;
    public Double IDMAGY = 0.0;
    public Double IDMAGZ = 0.0;

    public int IDRCAUX1 = 0;
    public int IDRCAUX2 = 0;
    public int IDRCAUX3 = 0;
    public int IDRCAUX4 = 0;
    public int IDRCPITCH = 0;
    public int IDRCROLL = 0;
    public int IDRCTHROTTLE = 0;
    public int IDRCYAW = 0;
    public int IDVBAT = 0;

    // For accessing values that do not change (often) over time , switches and
    // status
    public int UAVVERSION_KEY = 0;
    public int UAVTYPE_KEY = 1;
    public int RCRATE_KEY = 2;
    public int RCEXPO_KEY = 3;
    public int ROLLPITCHRATE_KEY = 4;
    public int YAWRATE_KEY = 5;
    public int MSPVERSION_KEY = 6;
    public int UAVCAPABILITY_KEY = 7;
    public int RCCURV_THRMID_KEY = 8;
    public int RCCURV_THREXPO_KEY = 9;
    public int POEWERTRIG_KEY = 10;
    public int DYNTHRPID_KEY = 11;

    private int MASK = 0xff;

    /**
     * reception buffer, don't make bigger than needed, we Object.clone() it
     * frequently.
     */
    private static final int BUFZ = 100;
    private static byte[] buffer = new byte[BUFZ]; // not final, replaced below

    /**
     * position in the reception inputBuffer
     */
    private static int offset;

    // Multiwii serial command byte definitions
    public static final int IDENT = 100, STATUS = 101, RAW_IMU = 102,
            SERVO = 103, MOTOR = 104, RC = 105, RAW_GPS = 106, COMP_GPS = 107,
            ATTITUDE = 108, ALTITUDE = 109, BAT = 110, RC_TUNING = 111,
            PID = 112, BOX = 113, MISC = 114, MOTOR_PINS = 115, BOXNAMES = 116,
            PIDNAMES = 117, SET_RAW_RC = 200, SET_RAW_GPS = 201, SET_PID = 202,
            SET_BOX = 203, SET_RC_TUNING = 204, ACC_CALIBRATION = 205,
            MAG_CALIBRATION = 206, SET_MISC = 207, RESET_CONF = 208,
            EEPROM_WRITE = 250, DEBUG = 254;

    // protocol header for reply packet
    private static final int MSP_IN_HEAD1 = '$';
    private static final int MSP_IN_HEAD2 = 'M';
    private static final int MSP_IN_HEAD3 = '>';

    // protocol header for command packet
    private static final byte[] MSP_OUT = { '$', 'M', '<' };

    /* status for the serial decoder */
    private static final int IDLE = 0, HEADER_START = 1, HEADER_M = 2,
            HEADER_ARROW = 3, HEADER_SIZE = 4, HEADER_CMD = 5;

    private static int mspState = IDLE; // reply decoder state

    private static int cmd; // incoming commande
    private static int dataSize; // size of the incoming payload
    private static int checksum; // checksum of the incoming message

    /** change state, optional "state transition" debug diagnostics */
    private void setState(int aState) {
        mspState = aState;
        // System.out.println( " state:"+aState );
    }
    // assemble a byte of the reply packet into "buffer"
    private void save(int aByte) {
        if (offset < buffer.length)
            buffer[offset++] = (byte) aByte;
    }

    /**
     * Class ByteBuffer is here so touching the data model can be done from the
     * event dispatching thread, rather than from the serial event thread (which
     * was causing a lockup, at least on linux). This holds a portion of the
     * command packet so method run() can process it on the event dispatching
     * (Swing GUI) thread.
     */
    class ByteBuffer extends ByteArrayInputStream implements
            Runnable {
//        final MwDataModel model;

        public ByteBuffer(byte[] input, int count) {
            super(input, 0, count);

//            model = MSP.model;
        }

        // read from "this" ByteArrayInputStream, 8 lower bits only, uppers are
        // zero
        final int read8() {
            return read();
        }

        final int read16() {
            int ret = read();
            // sign extend this byte
            ret |= ((byte) read()) << 8;
            return ret;
        }

        final int read32() {
            int ret = read();
            ret |= read() << 8;
            ret |= read() << 16;
            ret |= read() << 24;
            return ret;
        }

        public void run() {
            int cmd = read();
            Date d = new Date();
            Log.d("MSP:Result", "cmd : " + cmd);
            switch (cmd) {
                case IDENT:
//                    model.put(MSP.UAVVERSION_KEY, read8());
//                    model.put(MSP.UAVTYPE_KEY, read8());
//                    model.put(MSP.MSPVERSION_KEY, read8());
//                    model.put(MSP.UAVCAPABILITY_KEY, read32());
                    break;

                case STATUS:
                    int cycleTime = read16();
                    int i2cError = read16();

                    int present = read16();
                    int mode = read32();

                    Log.d("MSP:Status", "cycleTime: " + cycleTime);
                    Log.d("MSP:Status", "i2cError: " + i2cError);
                    Log.d("MSP:Status", "present: " + present);
                    Log.d("MSP:Status", "mode: " + mode);
                    if ((present & 1) > 0) {
                        // buttonAcc.setColorBackground(green_);
                    } else {
                        // buttonAcc.setColorBackground(red_);
                        // tACC_ROLL.setState(false);
                        // tACC_PITCH.setState(false);
                        // tACC_Z.setState(false);
                    }

                    if ((present & 2) > 0) {
                        // buttonBaro.setColorBackground(green_);
                    } else {
                        // buttonBaro.setColorBackground(red_);
                        // tBARO.setState(false);
                    }

                    if ((present & 4) > 0) {
                        // buttonMag.setColorBackground(green_);
                    } else {
                        // buttonMag.setColorBackground(red_);
                        // tMAGX.setState(false);
                        // tMAGY.setState(false);
                        // tMAGZ.setState(false);
                    }

                    if ((present & 8) > 0) {
                        // buttonGPS.setColorBackground(green_);
                    } else {
                        // buttonGPS.setColorBackground(red_);
                        // tHEAD.setState(false);
                    }

                    if ((present & 16) > 0) {
                        // buttonSonar.setColorBackground(green_);
                    } else {
                        // buttonSonar.setColorBackground(red_);
                    }

//                    for (int index = 0; index < model.getBoxNameCount(); index++) {
//                        model.setBoxNameState(index, ((mode & (1 << index)) > 0));
//                    }
                    break;

                case RAW_IMU:
                    IDAX = Double.valueOf(read16());
                    IDAY = Double.valueOf(read16());
                    IDAZ = Double.valueOf(read16());

                    IDGX = Double.valueOf(read16() / 8);
                    IDGY = Double.valueOf(read16() / 8);
                    IDGZ = Double.valueOf(read16() / 8);
                    IDMAGX = Double.valueOf(read16() / 3);
                    IDMAGY = Double.valueOf(read16() / 3);
                    IDMAGZ = Double.valueOf(read16() / 3);
                    break;

                case SERVO:
//                    for (int i = 0; i < MAXSERVO; i++) {
//                        model.getRealTimeData().put(d, "servo" + i,
//                                Double.valueOf(read16()), MwSensorClassServo.class);
//                    }
                    break;

                case MOTOR:
//                    for (int i = 0; i < MAXMOTOR; i++) {
//                        model.getRealTimeData().put(d, "mot" + i,
//                                Double.valueOf(read16()), MwSensorClassMotor.class);
//                    }
                    break;

                case RC:
                    IDRCROLL = read16();
                    IDRCPITCH = read16();
                    IDRCYAW = read16();
                    IDRCTHROTTLE = read16();
                    IDRCAUX1 = read16();
                    IDRCAUX2 = read16();
                    IDRCAUX3 = read16();
                    IDRCAUX4 = read16();
                    System.out.print(IDRCROLL);
                    System.out.print(IDRCPITCH);
                    System.out.print(IDRCYAW);
                    System.out.println(IDRCTHROTTLE);

                    break;

                case RAW_GPS:
                    // GPS_fix = read8();
                    // GPS_numSat = read8();
                    // GPS_latitude = read32();
                    // GPS_longitude = read32();
                    // GPS_altitude = read16();
                    // GPS_speed = read16();
                    break;

                case COMP_GPS:
                    // GPS_distanceToHome = read16();
                    // GPS_directionToHome = read16();
                    // GPS_update = read8();
                    break;

                case ATTITUDE:
//                    model.getRealTimeData().put(d, IDANGX,
//                            Double.valueOf(read16() / 10), MwSensorClassHUD.class);
//                    model.getRealTimeData().put(d, IDANGY,
//                            Double.valueOf(read16() / 10), MwSensorClassHUD.class);
//                    model.getRealTimeData().put(d, IDHEAD,
//                            Double.valueOf(read16()), MwSensorClassCompas.class);
                    break;

                case ALTITUDE:
//                    model.getRealTimeData().put(d, IDALT,
//                            Double.valueOf(read32()) / 100,
//                            MwSensorClassCompas.class);
                    break;

                case BAT: // TODO SEND
//                    model.getRealTimeData().put(d, IDVBAT, Double.valueOf(read8()),
//                            MwSensorClassPower.class);
//                    model.getRealTimeData().put(d, IDPOWERMETERSUM,
//                            Double.valueOf(read16()), MwSensorClassPower.class);
                    break;

                case RC_TUNING:
                    // Dividing an unsigned 8 bit value by 100, then converting
                    // back to int, leaves a resolution of only 1 part in
                    // 3 ( 0 to 2 ). 0 - 255 divided by 100 using integer math.
//                    model.put(MSP.RCRATE_KEY, (int) (read8() / 100.0));
//                    model.put(MSP.RCEXPO_KEY, (int) (read8() / 100.0));
//                    model.put(MSP.ROLLPITCHRATE_KEY, (int) (read8() / 100.0));
//                    model.put(MSP.YAWRATE_KEY, (int) (read8() / 100.0));
//                    model.put(MSP.DYNTHRPID_KEY, (int) (read8() / 100.0));
//                    model.put(MSP.RCCURV_THRMID_KEY, (int) (read8() / 100.0));
//                    model.put(MSP.RCCURV_THREXPO_KEY, (int) (read8() / 100.0));
                    break;

                case ACC_CALIBRATION:
                    break;

                case MAG_CALIBRATION:
                    break;

                case PID:
//                    for (int index = 0; index < model.getPidNameCount(); index++) {
//                        model.setPidValue(index, read8(), read8(), read8());
//                    }
//                    model.pidChanged();
                    break;

                case BOX:
//                    for (int index = 0; index < model.getBoxNameCount(); index++) {
//                        int bytread = read16();
//                        model.setBoxNameValue(index, bytread);
//                    }
//                    model.boxChanged();
                    break;

                case MISC: // TODO SEND
//                    model.put(MSP.POEWERTRIG_KEY, read16());
                    break;

                case MOTOR_PINS:// TODO SEND
                    for (int i = 0; i < 8; i++) {
//                        model.setMotorPin(i, read8());
                    }
                    break;

                case DEBUG:
                    for (int i = 1; i < 5; i++) {
//                        model.getRealTimeData().put(d, "debug" + i,
//                                Double.valueOf(read16()), MwSensorClassIMU.class);
                    }
                    break;

                case BOXNAMES:
//                    model.removeAllBoxName();
                {
                    int i = 0;
                    // start at index 1, because of cmd byte
                    for (String name : new String(buf, 1, available())
                            .split(";")) {
//                        model.addBoxName(name, i++);
                    }
                }
                break;

                case PIDNAMES:
//                    model.removeAllPIDName();
                {
                    int i = 0;
                    // start at index 1, because of cmd byte
                    for (String name : new String(buf, 1, available())
                            .split(";")) {
//                        model.addPIDName(name, i++);
                    }
                }
                break;
                default:
                {
                    for (int i = 0; i< dataSize ; i++) {
                        System.out.println("Data: " + read16());
                    }
                    break;
                }
            }
        }
    }
    public void decode(int input) {
        // LOGGER.trace("mspState = " + mspState + "\n");
        switch (mspState) {
            default:
                // mspState is at an unknown value, but this cannot happen
                // unless somebody introduces a bug.
                // fall thru just in case.

            case IDLE:
                setState(MSP_IN_HEAD1 == input ? HEADER_START : IDLE);
                break;

            case HEADER_START:
                setState(MSP_IN_HEAD2 == input ? HEADER_M : IDLE);
                break;

            case HEADER_M:
                setState(MSP_IN_HEAD3 == input ? HEADER_ARROW : IDLE);
                break;


            case HEADER_ARROW: // got arrow, expect dataSize now
                // This is the count of bytes which follow AFTER the command
                // byte which is next. +1 because we save() the cmd byte too, but
                // it excludes the checksum
                dataSize = input + 1;

                // reset index variables for save()
                offset = 0;
                checksum = input; // same as: checksum = 0, checksum ^= input;

                // the command is to follow
                setState(HEADER_SIZE);
                break;

            case HEADER_SIZE: // got size, expect cmd now
                cmd = input;
                checksum ^= input;

                // pass the command byte to the ByteBuffer handler also
                save(input);
                setState(HEADER_CMD);
                break;

            case HEADER_CMD: // got cmd, expect payload, if any, then checksum
                if (offset < dataSize) {
                    // keep reading the payload in this state until offset==dataSize
                    checksum ^= input;
                    save(input);

                    // stay in this state
                } else {
                    // done reading, reset the decoder for next byte
                    setState(IDLE);

                    if ((checksum & MASK) != input) {

//                        if (LOGGER.isTraceEnabled()) {
                        System.out.printf(
                                "checksum error, expected:%02x got:%02x\n",
                                checksum & 0xff, input);
//                        } else {
//                            LOGGER.error("invalid checksum for command " + cmd
//                                    + ": " + (checksum & MASK) + " expected, got "
//                                    + input + "\n");
//                        }
                    } else {
                        // Process the checksum verified command on the event
                        // dispatching
                        // thread. The checksum is omitted from ByteBuffer.
                        // Give up "buffer" to ByteBuffer, replace it below.
                        ByteBuffer bb = new ByteBuffer(buffer, offset);
                        bb.run();
                        // replace the buffer which we gave up to ByteBuffer
                        buffer = new byte[BUFZ];
                    }
                }
                break;
        }


    }
    public ByteArrayOutputStream request(int msp) {
        return request(msp, null);
    }

    // send msp with payload
    public ByteArrayOutputStream request(int msp, byte[] payload) {
        ByteArrayOutputStream bf = new ByteArrayOutputStream();

        bf.write(MSP_OUT, 0, MSP_OUT.length);

        int hash = 0; // upper 24 bits will be ignored.
        int payloadz = 0; // siZe

        if (payload != null)
            payloadz = payload.length;

        bf.write(payloadz);
        hash ^= payloadz;

        bf.write(msp);
        hash ^= msp;

        if (payload != null) {
            for (byte b : payload) {
                bf.write(b);
                hash ^= b;
            }
        }

        bf.write(hash);
        return bf;
    }
}
