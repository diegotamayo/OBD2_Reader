package com.example.obdapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import java.util.ArrayList;

public class ParametersActivity extends Activity {

    private ArrayList<String> chosenParams = new ArrayList<>();
    private int chosenParamsAmount = 0;

    // Speed (1)
    private CheckBox speed;
    // Command (1)
    private CheckBox permanentTroubleCodes;
    // Engine (3)
    private CheckBox engineLoad, rpm, throttlePosition;
    // Temperature (1)
    private CheckBox coolantTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parameters);

        Intent oncomingIntent = getIntent();
        chosenParams = (ArrayList<String>) oncomingIntent.getSerializableExtra("currentlySelectedParameters");
        chosenParamsAmount = oncomingIntent.getIntExtra("currentParametersAmount", 3);

        //region Checkboxes
        // Speed (1)
        speed = findViewById(R.id.cbSpeed);
        // Command (1)
        permanentTroubleCodes = findViewById(R.id.cbPermanentTroubleCodes);
        // Engine (3)
        engineLoad = findViewById(R.id.cbEngineLoad);
        rpm = findViewById(R.id.cbRpm);
        throttlePosition = findViewById(R.id.cbThrottlePosition);
        // Temperature (1)
        coolantTemp = findViewById(R.id.cbCoolantTemp);
        //endregion

        for(String paramName : chosenParams){
            switch(paramName){
                // Speed (1)
                case "Vehicle Speed":
                    speed.setChecked(true);
                    break;
                // Command (1)
                case "Permanent Trouble Codes":
                    permanentTroubleCodes.setChecked(true);
                    break;
                // Engine (3)
                case "Engine Load":
                    engineLoad.setChecked(true);
                    break;
                case "Engine RPM":
                    rpm.setChecked(true);
                    break;
                case "Throttle Position":
                    throttlePosition.setChecked(true);
                    break;
                // Temperature (3)
                case "Engine Coolant Temperature":
                    coolantTemp.setChecked(true);
                    break;
            }
        }

        // Speed (1)
        // Selecciona parámetro 'Velocidad'.
        speed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(speed.isChecked()){
                addParam("Vehicle Speed", speed);
            } else {
                removeParam("Vehicle Speed", speed);
            }
        });

        // Command (1)
        // Selecciona parámetro 'Permanent Trouble Codes'.
        permanentTroubleCodes.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(permanentTroubleCodes.isChecked()){
                addParam("Permanent Trouble Codes", permanentTroubleCodes);
            } else {
                removeParam("Permanent Trouble Codes", permanentTroubleCodes);
            }
        });

        // Engine (3)
        // Selecciona parámetro 'Carga del Motor'.
        engineLoad.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(engineLoad.isChecked()){
                addParam("Engine Load", engineLoad);
            } else {
                removeParam("Engine Load", engineLoad);
            }
        });

        // Selecciona parámetro 'RPM'.
        rpm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(rpm.isChecked()){
                addParam("Engine RPM", rpm);
            } else {
                removeParam("Engine RPM", rpm);
            }
        });

        // Selecciona parámetro 'Posición del Acelerador' (Controla la entrada de aire del motor).
        throttlePosition.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(throttlePosition.isChecked()){
                addParam("Throttle Position", throttlePosition);
            } else {
                removeParam("Throttle Position", throttlePosition);
            }
        });

        // Temperature (1)
        // Selecciona parámetro 'Temperatura del Refrigerante'.
        coolantTemp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(coolantTemp.isChecked()){
                addParam("Engine Coolant Temperature", coolantTemp);
            } else {
                removeParam("Engine Coolant Temperature", coolantTemp);
            }
        });

        Button bSave = findViewById(R.id.bDone);
        bSave.setOnClickListener(e -> finishAction());
    }

    private void addParam(String paramName, CheckBox parentBox){
        if(chosenParamsAmount <= 5){
            chosenParams.add(paramName);
            chosenParamsAmount++;
            parentBox.setChecked(true);
        } else {
            Toast.makeText(ParametersActivity.this, "Can't add more than 5 parameters", Toast.LENGTH_LONG).show();
            parentBox.setChecked(false);
        }
    }

    private void removeParam(String paramName, CheckBox parentBox){
        if(chosenParamsAmount > 1){
            chosenParams.remove(paramName);
            chosenParamsAmount--;
            parentBox.setChecked(false);
        } else {
            Toast.makeText(ParametersActivity.this, "There must be at least one parameter to display", Toast.LENGTH_LONG).show();
            parentBox.setChecked(true);
        }
    }

    public void finishAction(){
        Intent output = new Intent();
        output.putExtra("parametersAmount", chosenParamsAmount);
        output.putExtra("parameters", chosenParams);

        setResult(RESULT_OK, output);
        finish();
    }
}