void readCurrentAndVoltage()
{
  float voltage_raw =   (5.0 / 1023.0) * analogRead(currentPin); // Read the voltage from sensor
  voltage =  voltage_raw - QOV + 0.012 ;// 0.000 is a value to make voltage zero when there is no current
  current = voltage / sensitivity;
  float sensorValue = analogRead(voltagePin);
  vOUT = (sensorValue * 5.0) / 1024.0;
  vIN = vOUT / (R2 / (R1 + R2));
}
