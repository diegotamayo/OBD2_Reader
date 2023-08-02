package com.example.obdapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.LoadCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.ThrottlePositionCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket btSocket;
    private String chosenDeviceName, chosenDeviceAddress;

    //region Declaraciones Buttons
    private Button bConnect;
    private Button bStart;
    private Button bStop;
    private Button bChooseDevice;
    private Button bExitApp;
    //endregion

    //region Declaraciones Labels
    // Speed Labels (1)
    private TextView command_SpeedLabel;
    // Engine Labels (3)
    private TextView command_LoadLabel, command_RPMLabel, command_ThrottlePositionLabel;
    // Temperature Labels (1)
    private TextView command_EngineCoolantTemperatureLabel;
    //endregion

    //region Declaraciones Results
    // Speed Results (1)
    private TextView command_SpeedResult;
    // Engine Results (3)
    private TextView command_LoadResult, command_RPMResult, command_ThrottlePositionResult;
    // Temperature Results (1)
    private TextView command_EngineCoolantTemperatureResult;
    //endregion

    //region Instanciación Commands
    // Speed (1)
    private ObdCommand command_Speed = new SpeedCommand();
    // Engine (3)
    private ObdCommand command_Load = new LoadCommand();
    private ObdCommand command_RPM = new RPMCommand();
    private ObdCommand command_ThrottlePosition = new ThrottlePositionCommand();
    // Temperature (1)
    private ObdCommand command_EngineCoolantTemperature = new EngineCoolantTemperatureCommand();
    //endregion

    private final ArrayList<ObdCommand> chosenParameters = new ArrayList<ObdCommand>() {
        {   /* Speed (1) */
            add(command_Speed);
            /* Engine (3) */
            add(command_Load);
            add(command_RPM);
            add(command_ThrottlePosition);
            /* Temperature (3) */
            add(command_EngineCoolantTemperature);
            //endregion
        }
    };

    private Timer myTimer;

    // Método principal de la actividad.
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //region Asociaciones Command Labels
        // Speed (1)
        command_SpeedLabel = findViewById(R.id.command_SpeedLabel);
        // Engine (3)
        command_LoadLabel = findViewById(R.id.command_EngineLoadLabel);
        command_RPMLabel = findViewById(R.id.command_RPMLabel);
        command_ThrottlePositionLabel = findViewById(R.id.command_ThrottlePositionLabel);
        // Temperature (1)
        command_EngineCoolantTemperatureLabel = findViewById(R.id.command_EngineCoolantTemperatureLabel);
        //endregion

        //region Asociaciones Command Results
        // Speed (1)
        command_SpeedResult = findViewById(R.id.command_SpeedResult);
        // Engine (3)
        command_LoadResult = findViewById(R.id.command_EngineLoadResult);
        command_RPMResult = findViewById(R.id.command_RPMResult);
        command_ThrottlePositionResult = findViewById(R.id.command_ThrottlePositionResult);
        // Temperature (1)
        command_EngineCoolantTemperatureResult = findViewById(R.id.command_EngineCoolantTemperatureResult);
        //endregion

        //region Asociaciones Buttons
        bChooseDevice = findViewById(R.id.bChooseDevice);
        bConnect = findViewById(R.id.bConnect);
        bStart = findViewById(R.id.bStart);
        bStop = findViewById(R.id.bStop);
        bExitApp = findViewById(R.id.bExitApp);
        //endregion

        //region Buttons Click Events
        bChooseDevice.setOnClickListener(e -> chooseBluetoothDevice());
        bConnect.setOnClickListener(e -> connectOBD());

        bStart.setOnClickListener(e -> {
            // Se inician plugins de AWS.
            try {
                Amplify.addPlugin(new AWSCognitoAuthPlugin());
                Amplify.addPlugin(new AWSS3StoragePlugin());
                Amplify.configure(getApplicationContext());
                // Se muestran mensajes de notificación de inicio de plugins AWS.
                Log.i("MyAmplifyApp", "Initialized Amplify");
                Toast.makeText(this, "Servicio AWS inicializado...", Toast.LENGTH_SHORT).show();
                // Se inicia lectura de datos.
                startOBD();
            } catch (AmplifyException amplifyException) {
                Log.e("MyAmplifyApp", "Could not initialize Amplify", amplifyException);
                Toast.makeText(getApplicationContext(), "AmplifyException: Could not initialize Amplify... " + amplifyException.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (RuntimeException runtimeException) {
                Toast.makeText(getApplicationContext(), "RuntimeException: Could not initialize Amplify... " + runtimeException.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        bStop.setOnClickListener(e -> {
            try {
                stopOBD();
            } catch (RuntimeException runtimeException) {
                Toast.makeText(getApplicationContext(), "RuntimeException: Could not initialize Amplify... " + runtimeException.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        bExitApp.setOnClickListener(e -> exitApp());
        //endregion

        //region Button Properties
        bConnect.setEnabled(false);
        bStart.setEnabled(false);
        bStop.setEnabled(false);
        //endregion
    }

    // Método para escribir la cabecera del archivo csv.
    private void writeCsvHeader(FileWriter fileWriter) {
        try {
            String line = String.format("%s, %s, %s, %s, %s\n", "Velocity", "Engine_Load", "RPM", "Throttle_Position", "Engine_Coolant_Temperature");
            fileWriter.write(line);
        } catch (IOException ioException) {
            Toast.makeText(getApplicationContext(), "IOException: Could not write Csv Header... " + ioException.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Método para escribir las unidades de la fila de cabecera del archivo csv.
    private void writeCsvHeaderUnits(FileWriter fileWriter) {
        try {
            // Lectura de las unidades para 'Velocidad'.
            command_Speed.run(btSocket.getInputStream(), btSocket.getOutputStream());
            String strSpeedUnit = command_Speed.getResultUnit();
            // Lectura de las unidades para 'Carga del motor'.
            command_Load.run(btSocket.getInputStream(), btSocket.getOutputStream());
            String strLoadUnit = command_Load.getResultUnit();
            // Lectura de las unidades para 'RPM'.
            command_RPM.run(btSocket.getInputStream(), btSocket.getOutputStream());
            String strRPMUnit = command_RPM.getResultUnit();
            // Lectura de las unidades para 'Posición del acelerador'.
            command_ThrottlePosition.run(btSocket.getInputStream(), btSocket.getOutputStream());
            String strThrottlePositionUnits = command_ThrottlePosition.getResultUnit();
            // Lectura de las unidades para 'Temperatura del refrigerante'.
            command_EngineCoolantTemperature.run(btSocket.getInputStream(), btSocket.getOutputStream());
            String strEngineCoolantTemperature = command_EngineCoolantTemperature.getResultUnit();
            String line = String.format("%s, %s, %s, %s, %s\n", strSpeedUnit, strLoadUnit, strRPMUnit, strThrottlePositionUnits, strEngineCoolantTemperature);
            fileWriter.write(line);
        } catch (IOException ioException) {
            Toast.makeText(getApplicationContext(), "IOException: Could not write Csv Header... " + ioException.getMessage(), Toast.LENGTH_LONG).show();
        } catch (InterruptedException interruptedException) {
        Toast.makeText(getApplicationContext(), "InterruptedException: Could not write Csv Header... " + interruptedException.getMessage(), Toast.LENGTH_LONG).show();
    }
    }

    // Método para escribir filas en el archivo csv.
    private void writeCsvData(float velocity, float engine_load, float rpm, float throttle_position, float engine_coolant_temperature, FileWriter fileWriter) {
        try {
            @SuppressLint("DefaultLocale") String line = String.format("%.2f,%.2f,%.2f,%.2f,%.2f\n", velocity, engine_load, rpm, throttle_position, engine_coolant_temperature);
            fileWriter.write(line);
        } catch (IOException ioException) {
            Toast.makeText(getApplicationContext(), "IOException: Could not write Csv Data... " + ioException.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Método para registrar resultados de actividades.
    ActivityResultLauncher<Intent> getResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent intent = result.getData();
                        assert intent != null;
                        String data = intent.getStringExtra("requestCode");
                        if (data.equals("ENABLE_BT_REQUEST")) {
                            continueBluetooth();
                        }
                    }
                    if (result.getResultCode() == RESULT_CANCELED) {
                        Intent intent = result.getData();
                        assert intent != null;
                        String data = intent.getStringExtra("requestCode");
                        if (data.equals("ENABLE_BT_REQUEST")) {
                            Toast.makeText(MainActivity.this, "M1 Application requires Bluetooth enabled", Toast.LENGTH_LONG).show();
                        }
                    }
                    if (result.getResultCode() == RESULT_OK) {
                        Intent intent = result.getData();
                        if (intent != null) {
                            ArrayList<String> newParametersNames = (ArrayList<String>) intent.getSerializableExtra("parameters");
                            chosenParameters.clear();
                            //region Options
                            for (String paramName : newParametersNames) {
                                switch (paramName) {
                                    //region Parameters
                                    // Speed (1)
                                    case "Vehicle Speed":
                                        chosenParameters.add(new SpeedCommand());
                                        break;
                                    // Engine (3)
                                    case "Engine Load":
                                        chosenParameters.add(new LoadCommand());
                                        break;
                                    case "Engine RPM":
                                        chosenParameters.add(new RPMCommand());
                                        break;
                                    case "Throttle Position":
                                        chosenParameters.add(new ThrottlePositionCommand());
                                        break;
                                    // Temperature (1)
                                    case "Engine Coolant Temperature":
                                        chosenParameters.add(new EngineCoolantTemperatureCommand());
                                        break;
                                    //endregion
                                }
                            }
                            try {
                                command_SpeedLabel.setText(String.format("%s:", chosenParameters.get(1).getName()));
                                command_Speed = chosenParameters.get(1);
                            } catch (IndexOutOfBoundsException ex) {
                                command_SpeedLabel.setText("");
                                command_Speed = null;
                            }
                            try {
                                command_LoadLabel.setText(String.format("%s:", chosenParameters.get(2).getName()));
                                command_Load = chosenParameters.get(2);
                            } catch (IndexOutOfBoundsException ex) {
                                command_LoadLabel.setText("");
                                command_Load = null;
                            }
                            try {
                                command_RPMLabel.setText(String.format("%s:", chosenParameters.get(3).getName()));
                                command_RPM = chosenParameters.get(3);
                            } catch (IndexOutOfBoundsException ex) {
                                command_RPMLabel.setText("");
                                command_RPM = null;
                            }
                            try {
                                command_ThrottlePositionLabel.setText(String.format("%s:", chosenParameters.get(4).getName()));
                                command_ThrottlePosition = chosenParameters.get(4);
                            } catch (IndexOutOfBoundsException ex) {
                                command_ThrottlePositionLabel.setText("");
                                command_ThrottlePosition = null;
                            }
                            try {
                                command_EngineCoolantTemperatureLabel.setText(String.format("%s:", chosenParameters.get(5).getName()));
                                command_EngineCoolantTemperature = chosenParameters.get(5);
                            } catch (IndexOutOfBoundsException ex) {
                                command_EngineCoolantTemperatureLabel.setText("");
                                command_EngineCoolantTemperature = null;
                            }
                            //endregion
                        } else {
                            Toast.makeText(MainActivity.this, "M2 Preferred parameters not saved correctly", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });

    // Método para seleccionar dispositivo Bluetooth.
    private void chooseBluetoothDevice() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "M3 Device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
        }
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.putExtra("requestCode", "ENABLE_BT_REQUEST");
            getResult.launch(enableBtIntent);
        } else {
            continueBluetooth();
        }
    }

    // Método complementario en la selección del dispositivo Bluetooth.
    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    private void continueBluetooth() {
        final ArrayList<String> pairedDevicesNames = new ArrayList<>();
        final ArrayList<String> pairedDevicesAddresses = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    pairedDevicesNames.add(device.getName());
                    pairedDevicesAddresses.add(device.getAddress());
                }

                final String[] devicesString = pairedDevicesNames.toArray(new String[0]);
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                mBuilder.setTitle("Seleccionar dispositivo OBD:");
                mBuilder.setSingleChoiceItems(devicesString, -1, (dialog, i) -> {
                    dialog.dismiss();
                    int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    chosenDeviceAddress = pairedDevicesAddresses.get(position);
                    chosenDeviceName = pairedDevicesNames.get(position);
                    Toast.makeText(MainActivity.this, "Dispositivo elegido: " + chosenDeviceName, Toast.LENGTH_SHORT).show();
                    TextView info = findViewById(R.id.info);
                    info.setText(String.format("Dispositivo: %s\tDirección: %s", chosenDeviceName, chosenDeviceAddress));
                    bConnect.setEnabled(true);

                });

                AlertDialog mDialog = mBuilder.create();
                mDialog.show();
            } else {
                Toast.makeText(getApplicationContext(), "No se encontraron dispositivos emparejados...", Toast.LENGTH_SHORT).show();
            }
        }

    }

    // Método para conectar el dispositivo Bluetooth seleccionado.
    private void connectOBD() {
        try {
            BluetoothDevice device = btAdapter.getRemoteDevice(chosenDeviceAddress);
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                btSocket = device.createRfcommSocketToServiceRecord(uuid);
                btSocket.connect();

                new EchoOffCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());
                new LineFeedOffCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());
                new SelectProtocolCommand(ObdProtocols.AUTO).run(btSocket.getInputStream(), btSocket.getOutputStream());

                Toast.makeText(MainActivity.this, "Conectado al dispositivo OBD...", Toast.LENGTH_SHORT).show();
                bStart.setEnabled(true);
                bConnect.setEnabled(false);
            }
        } catch (IllegalArgumentException e) {
            Toast.makeText(MainActivity.this, "Seleccione primero el dispositvo Bluetooth... ", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "No se puede establecer conexión... ", Toast.LENGTH_LONG).show();
        } catch (Exception e){
            Toast.makeText(MainActivity.this, "An error ocurred in OBD connection... " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    // Se construye el nombre del archivo con el formato "yyyyMMdd_HHmmss".
    @SuppressLint("SimpleDateFormat") SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd_HHmmss");
    Date date = new Date();
    String nombreArchivo = fmt.format(date) + ".csv";

    // Se definen variables para File y FileWriter.
    private File file;
    private FileWriter fileWriter;

    // Método que inicia la lectura desde la interfaz OBD-II.
    private void startOBD() {
        try {
            command_SpeedResult.setText("");
            command_LoadResult.setText("");
            command_RPMResult.setText("");
            command_ThrottlePositionResult.setText("");
            command_EngineCoolantTemperatureResult.setText("");

            bStart.setEnabled(false);
            bConnect.setEnabled(false);
            bChooseDevice.setEnabled(false);
            bStop.setEnabled(true);

            // Se crea archivo para almacenar los datos leídos.
            file = new File(getFilesDir(), nombreArchivo);
            fileWriter = new FileWriter(file);

            writeCsvHeader(fileWriter);
            writeCsvHeaderUnits(fileWriter);
            long frecuencia = 5000;
            myTimer = new Timer();

            myTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    // Mensaje en consola.
                    Log.i("Lectura realizada", "Fecha de la lectura: " + new Date());
                    try {
                        // Lectura de datos de 'Velocidad'.
                        command_Speed.run(btSocket.getInputStream(), btSocket.getOutputStream());
                        command_SpeedResult.setText(command_Speed.getCalculatedResult());
                        float velocidad = Float.parseFloat(command_SpeedResult.getText().toString());
                        // Lectura de datos de 'Carga del motor'.
                        command_Load.run(btSocket.getInputStream(), btSocket.getOutputStream());
                        command_LoadResult.setText(command_Load.getCalculatedResult());
                        float engineLoad = Float.parseFloat(command_LoadResult.getText().toString());
                        // Lectura de datos de 'RPM'.
                        command_RPM.run(btSocket.getInputStream(), btSocket.getOutputStream());
                        command_RPMResult.setText(command_RPM.getCalculatedResult());
                        float rpmResult = Float.parseFloat(command_RPMResult.getText().toString());
                        // Lectura de datos de 'Posición del acelerador'.
                        command_ThrottlePosition.run(btSocket.getInputStream(), btSocket.getOutputStream());
                        command_ThrottlePositionResult.setText(command_ThrottlePosition.getCalculatedResult());
                        float throttlePosition = Float.parseFloat(command_ThrottlePositionResult.getText().toString());
                        // Lectura de datos de 'Temperatura del refrigerante'.
                        command_EngineCoolantTemperature.run(btSocket.getInputStream(), btSocket.getOutputStream());
                        command_EngineCoolantTemperatureResult.setText(command_EngineCoolantTemperature.getCalculatedResult());
                        float engineCoolantTemperature = Float.parseFloat(command_EngineCoolantTemperatureResult.getText().toString());
                        // Se escriben los datos de la lectura en una nueva fila del archivo.
                        writeCsvData(velocidad, engineLoad, rpmResult, throttlePosition, engineCoolantTemperature, fileWriter);
                    } catch (RuntimeException runtimeException) {
                        Toast.makeText(getApplicationContext(), "RuntimeException: Could not initialize Timer... " + runtimeException.getMessage(), Toast.LENGTH_LONG).show();
                    } catch (IOException ioException) {
                        Toast.makeText(getApplicationContext(), "IOException: Could not initialize Timer... " + ioException.getMessage(), Toast.LENGTH_LONG).show();
                    } catch (InterruptedException interruptedException) {
                        Toast.makeText(getApplicationContext(), "InterruptedException: Could not initialize Timer... " + interruptedException.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }, 0, frecuencia);
        } catch (IOException ioException) {
            Toast.makeText(getApplicationContext(), "IOException: Could not start data reading... " + ioException.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Método que detiene la lectura desde la interfaz OBD-II.
    private void stopOBD() {
        // Se finaliza el objeto 'Timer'.
        myTimer.cancel();
        // Se limpian los campos de las lecturas.
        command_SpeedResult.setText("");
        command_LoadResult.setText("");
        command_RPMResult.setText("");
        command_ThrottlePositionResult.setText("");
        command_EngineCoolantTemperatureResult.setText("");
        // Se muestra un mensaje de confirmación del archivo guardado.
        Toast.makeText(getApplicationContext(), "Archivo " + file + " guardado con éxito.", Toast.LENGTH_LONG).show();

        try {
            bStart.setEnabled(false);
            bStop.setEnabled(false);
            bConnect.setEnabled(false);
            bChooseDevice.setEnabled(false);
            // Se sube el archivo generado a AWS.
            uploadFile(nombreArchivo, file);
            // Se liberan los recursos del archivo generado.
            assert fileWriter != null;
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException ioException) {
            Toast.makeText(getApplicationContext(), "IOException: Could not stop data reading... " + ioException.getMessage(), Toast.LENGTH_LONG).show();
        } catch (RuntimeException runtimeException) {
            Toast.makeText(getApplicationContext(), "RuntimeException: Could not stop data reading... " + runtimeException.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Método para subir archivo a repositorio en AWS.
    private void uploadFile(String nombreArchivo, File file) {
        try {
            Amplify.Storage.uploadFile(
                    nombreArchivo,
                    file,
                    result -> Toast.makeText(getApplicationContext(), "Archivo subido a Amazon S3 con éxito", Toast.LENGTH_LONG).show(),
                    storageFailure -> Log.i("MyAmplifyApp", "Upload failed", storageFailure)
            );
        } catch (RuntimeException runtimeException) {
            Toast.makeText(getApplicationContext(), "RuntimeException: Could not upload file to AWS... " + runtimeException.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Método para salir de la aplicación.
    private void exitApp() {
        try {
            MainActivity.this.finish();
            System.exit(0);
        } catch (Exception exception){
            Toast.makeText(getApplicationContext(), "An error ocurred when trying exit app... " + exception.toString(), Toast.LENGTH_LONG).show();
        }
    }
}