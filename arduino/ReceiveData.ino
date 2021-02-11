void recvData()
{
  
  if (Serial.available() > 0) {
    char inData[1000];
  char inChar = -1;
  byte index = 0;
//    Serial.print(Serial.read());
    DeserializationError error = deserializeJson(doc, Serial);

    // Test if parsing succeeds.
    if (error) {
       Serial.print(F("deserializeJson() failed: "));
       Serial.println(error.c_str());
      return;
    }
 
    data.rt = doc["rt"];
    data.ry = doc["ry"];
    data.rp = doc["rp"];
    data.rr = doc["rr"];
    data.r1 = doc["r1"];
    data.r2 = doc["r2"];
    rcControl = doc["irc"];
    setPPMValuesFromData();
    Serial.print("received");
 
//    if(data.r1 == 1500){
//      digitalWrite(3,HIGH);
//    } else{
//      digitalWrite(3,LOW);
//    }
//    if(data.r2 == 1500) {
//      digitalWrite(4,HIGH);
//    } else {
//      digitalWrite(4,LOW);
//    }
  }
}
