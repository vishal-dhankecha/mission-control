// SPI - Version: Latest
#include <SPI.h>
//
#include <SoftwareSerial.h>
//  
SoftwareSerial telemetry(10, 11); // RX, TX

float alt, AccX, AccY, AccZ= 0.0;
int16_t Temp1,Temp2, Rpm, Speed;
char Lat[50],Long[50];

// ArduinoJson - Version: Latest
#include <ArduinoJson.h>

// Used to communicate with android
StaticJsonDocument<500> doc;
//StaticJsonDocument<200> doc2;

#define clockMultiplier 2 // set this to 2 if you are using a 16MHz arduino, leave as 1 for an 8MHz arduino

//////////////////////////// Voltage Sensor variables/////////////
#define voltagePin A6
float vOUT = 0.0;
float vIN = 0.0;
float R1 = 30000.0;
float R2 = 7500.0;

//////////////////////////////////////////////////////////////////
//////////////////////////// Current Sensor variables/////////////
#define currentPin A7 // define the Arduino pin A0 as voltage input (V in)
const float VCC   = 5.0;// supply voltage is from 4.5 to 5.5V. Normally 5V.
float cutOffLimit = 1.01;// set the current which below that value, doesn't matter. Or set 0.5

/*
          "ACS712ELCTR-05B-T",// for model use 0
          "ACS712ELCTR-20A-T",// for model use 1
          "ACS712ELCTR-30A-T"// for model use 2
  sensitivity array is holding the sensitivy of the  ACS712
  current sensors. Do not change. All values are from page 5  of data sheet
*/
float sensitivity = 0.066;// for ACS712ELCTR-30A-T
const float QOV =   0.5 * VCC;// set quiescent Output voltage of 0.5V
float voltage, current;// internal variable for voltage

//////////////////////////////////////////////////////////////////////////////

unsigned long previousMillis = 0, prvMilsHighFrequency = 0, oneSPrvMillis=0;
const long dataSendInterval = 300;  

bool rcControl=false;


// The sizeof this struct should not exceed 32 bytes
struct MyData {
  int16_t rt;
  int16_t ry;
  int16_t rp;
  int16_t rr;
  int16_t r1;
  int16_t r2;
};

MyData data;

void resetData()
{
  // 'safe' values to use when no radio input is detected
  data.rt = 1000;
  data.ry = 1500;
  data.rp = 1500;
  data.rr = 1500;
  data.r1 = 1000;
  data.r2 = 1000;
}

void setup() {
  delay(2000);
  resetData();
  setupPPM();
//  pinMode(voltagePin, INPUT);
//  pinMode(currentPin, INPUT);

  Serial.begin(9600);

  telemetry.begin(9600); 
  setupRCDataRead();
  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(3, OUTPUT);
  pinMode(4, OUTPUT);
  digitalWrite(3, LOW);
  digitalWrite(4, LOW);
  digitalWrite(LED_BUILTIN, LOW);
}

void loop() {
  recvData();
  if(rcControl)
  {
    readRCData();
  }
  // readCurrentAndVoltage();
  readTelemetry();
  setPPMValuesFromData();
  sendData();
}
