void sendData()
{
  unsigned long currentMillis = millis();
  if (currentMillis - previousMillis >= dataSendInterval) {
    previousMillis = currentMillis;
    char voltStr[8];
    char currentStr[8];
    char altStr[10];
    char ax[8];
    char ay[8];
    char az[8];

    dtostrf(vIN, 6, 2, voltStr);
    dtostrf(current, 6, 2, currentStr);
    dtostrf(alt, 8, 2, altStr);
    dtostrf(AccX, 6, 2, ax);
    dtostrf(AccY, 6, 2, ay);
    dtostrf(AccZ, 6, 2, az);
    char output[400];
    

    sprintf(output,"{\"voltage\":%s,\"al\":%s,\"te\":%d,\"te2\":%d,\"ax\":%s,\"ay\":%s,\"az\":%s,\"lat\":\"%s\",\"lon\":\"%s\",\"spe\":%d}",voltStr, altStr, Temp1, Temp2, ax, ay, az, Lat, Long, Speed);
    Serial.println(output);
  }
}
