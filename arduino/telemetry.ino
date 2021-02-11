#define ID_ACC_X              0x24
#define ID_ACC_Y              0x25
#define ID_ACC_Z              0x26
#define ID_TEMPRATURE1        0x02
#define ID_TEMPRATURE2        0x05
#define ID_RPM                0x03

#define ID_GPS_SPEED_BP       0x11
#define ID_GPS_SPEED_AP       0x19
#define ID_LONGITUDE_BP       0x12
#define ID_LONGITUDE_AP       0x1A
#define ID_E_W                0x22
#define ID_LATITUDE_BP        0x13
#define ID_LATITUDE_AP        0x1B


void readTelemetry()
{
 static byte buffer[100];
 static int idx = 0;
 int i;

 while (telemetry.available ()) {
   buffer[idx] = telemetry.read ();
   if (buffer[idx-1] == 0x5E && buffer[idx] == 0x5E) {
     decode_frame (buffer, idx);
     buffer[0] = 0x5E;
     idx = 1;
   } else {
     idx++;
     if (idx > 100) {  // error
       buffer[0] = 0xE;
       idx = 1;
     }
   }
 }
}

void decode_frame (byte *buffer, int length) {
 int i = 1;
 
 int16_t        alt_a, gps_alt_a, gps_course_a, gps_lat_a, gps_long_a, gps_speed_a, voltage_a;
 static int16_t alt_b, gps_alt_b, gps_course_b, gps_lat_b, gps_long_b, gps_speed_b, voltage_b;
 
 while (i < length) {

   switch (buffer[i]) {

     case ID_ACC_X:         AccX = (FrskyD.decodeInt (&buffer[i+1]) / 1000.0); break;
     case ID_ACC_Y:         AccY = (FrskyD.decodeInt (&buffer[i+1]) / 1000.0); break;
     case ID_ACC_Z:         AccZ = (FrskyD.decodeInt (&buffer[i+1]) / 1000.0); break;

     case 0x10:        alt_b = FrskyD.decodeInt (&buffer[i+1]); break;
     case 0x21:        alt_a = FrskyD.decodeInt (&buffer[i+1]); alt = FrskyD.calcFloat (alt_b, alt_a); break;
     
     case ID_RPM:          Rpm = (FrskyD.decodeInt (&buffer[i+1])); break;
     case ID_TEMPRATURE1:         Temp1 = (FrskyD.decodeInt (&buffer[i+1])); break;
     case ID_TEMPRATURE2:        Temp2 = (FrskyD.decodeInt (&buffer[i+1])); break;

     case ID_LATITUDE_BP:    gps_lat_b = FrskyD.decodeInt (&buffer[i+1]);
                             break;
     case ID_LATITUDE_AP:    gps_lat_a = FrskyD.decodeInt (&buffer[i+1]);
                             sprintf(Lat, "%i.%i", gps_lat_b,gps_lat_a);
                             break;
     
     case ID_LONGITUDE_BP:   gps_long_b = FrskyD.decodeInt (&buffer[i+1]); break;
      
     case ID_LONGITUDE_AP:  gps_long_a = FrskyD.decodeInt (&buffer[i+1]);
                            sprintf(Long, "%i.%i", gps_long_b,gps_long_a);
                            break;
     case ID_GPS_SPEED_BP:  gps_speed_b = FrskyD.decodeInt (&buffer[i+1]); break;
     case ID_GPS_SPEED_AP:  gps_speed_b = FrskyD.decodeInt (&buffer[i+1]);
                            Speed = FrskyD.calcFloat (gps_speed_b, gps_speed_a);
                            break;

   }
   i++;
   while (buffer[i-1] != 0x5E) i++;
 }
}
