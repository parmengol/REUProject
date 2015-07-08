package edu.fiu.mpact.reuproject;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class IntentService extends android.app.IntentService {
    static Process p = null;
    String[] ouiList;
    Random gen = new Random();
    char[] charList = {'A', 'B', 'C', 'D', 'E', 'F', '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9'};


    public IntentService() {
        super("IntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        try {
            p = Runtime.getRuntime().exec("su");  // prompt for root access
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(checkIfGranted()) {
            try {
                ouiList = loadOUIs();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d("my log grant", "" + checkIfGranted());

            new Timer().scheduleAtFixedRate(new TimerTask() {
                BaseActivity b = new BaseActivity();

                @Override
                public void run() {
                    Log.d("my log2", b.getStatus() + "");

                    try {
                        if (!b.getStatus() && !LocalizeActivity.readyToSync()) {
                            Log.d("interface","" + getActiveWifiInterface(getApplicationContext()));
                            changeMac();
                        } else {
                            Log.d("my log", "ready to scan");
                        }

                        Log.d("my log3", Utils2.getMACAddress("wlan0"));
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 3000); // changes MAC every 3 seconds


        }
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

    public static boolean checkIfGranted(){

            boolean retval = false;


            try
            {

                DataOutputStream os = new DataOutputStream(p.getOutputStream());
                DataInputStream osRes = new DataInputStream(p.getInputStream());

                if (null != os && null != osRes)
                {
                    // Getting the id of the current user to check if this is root
                    os.writeBytes("id\n");
                    os.flush();

                    String currUid = osRes.readLine();
                    boolean exitSu = false;
                    if (null == currUid)
                    {
                        retval = false;
                        exitSu = false;
                        Log.d("ROOT", "Can't get root access or denied by user");
                    }
                    else if (true == currUid.contains("uid=0"))
                    {
                        retval = true;
                        exitSu = true;
                        Log.d("ROOT", "Root access granted");
                    }
                    else
                    {
                        retval = false;
                        exitSu = true;
                        Log.d("ROOT", "Root access rejected: " + currUid);
                    }

                }
            }
            catch (Exception e)
            {
                // Can't get root !
                // Probably broken pipe exception on trying to write to output stream (os) after su failed, meaning that the device is not rooted

                retval = false;
                Log.d("ROOT", "Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
            }

            return retval;
        }
    public static NetworkInterface getActiveWifiInterface(Context context) throws SocketException, UnknownHostException {
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        //Return dynamic information about the current Wi-Fi connection, if any is active.
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if(wifiInfo == null){
            Log.d("my log", "wifi null");
            return null;
        }
        InetAddress address = intToInet(wifiInfo.getIpAddress());
        return NetworkInterface.getByInetAddress(address);
    }

    public static byte byteOfInt(int value, int which) {
        int shift = which * 8;
        return (byte)(value >> shift);
    }

    public static InetAddress intToInet(int value) {
        byte[] bytes = new byte[4];
        for(int i = 0; i<4; i++) {
            bytes[i] = byteOfInt(value, i);
        }
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            // This only happens if the byte array has a bad length
            return null;
        }
    }
    }



