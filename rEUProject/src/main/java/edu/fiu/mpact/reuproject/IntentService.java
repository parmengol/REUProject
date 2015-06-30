package edu.fiu.mpact.reuproject;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class IntentService extends android.app.IntentService {
    Process p = null;
    String[] ouiList;
    Random gen = new Random();
    char[] charList = {'A', 'B', 'C', 'D', 'E', 'F', '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9'};


    public IntentService() {
        super("MAC");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            ouiList = loadOUIs();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            p = Runtime.getRuntime().exec("su");  // prompt for root access
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Timer().scheduleAtFixedRate(new TimerTask() {
            BaseActivity b = new BaseActivity();

            @Override
            public void run() {
              Log.d("my log2", b.getStatus() + "");

                try {
                    if(!b.getStatus()) {
                        changeMac();
                    }

                    Log.d("my log3", Utils2.getMACAddress("wlan0"));
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 5000); // changes MAC every 3 seconds



    }
    private void changeMac() throws IOException, InterruptedException {
        String mac = generateMac();

        //commands to execute
        String[] cmds = {"ip link set wlan0 address " + mac};

        // execute the commands
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        for (String tmpCmd : cmds) {
            os.writeBytes(tmpCmd + "\n");
        }

    }
    private String generateMac(){
        String s = ouiList[gen.nextInt(20847)] + ":";

        for(int i = 0; i < 6; i++){
            s = s + charList[gen.nextInt(16)];

            //add colon
            if(((i + 1) % 2 == 0) && i != 5){
                s = s + ":";
            }
        }

        return s;
    }

    private String[] loadOUIs() throws IOException {
        String[] ouiList = new String[20847];

        int i = 0;
        InputStream inStream = getApplicationContext().getResources().openRawResource(R.raw.oui2);
        InputStreamReader is = new InputStreamReader(inStream);
        BufferedReader reader = new BufferedReader(is);

        String word = reader.readLine();  //read first OUI
        while(word != null){             //continue until no more OUI's
            ouiList[i] = word;
            word = reader.readLine();
            i++;
        }

        return ouiList;

    }


}
