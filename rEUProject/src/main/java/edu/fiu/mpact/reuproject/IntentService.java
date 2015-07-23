package edu.fiu.mpact.reuproject;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Path;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;


public class IntentService extends android.app.IntentService {
    static Process p = null;
    String[] ouiList;
    public static final String PREFS_NAME = "Interface";
    Random gen = new Random();
    //String inter = "";  //the interface
    char[] charList = {'A', 'B', 'C', 'D', 'E', 'F', '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9'};
    static String theInterface;
    Uri uri = null;
   static PrintWriter myFile = null;
    final String filename = "Spoofed MACs";
    File thePath;
    static ArrayList<String> macList = new ArrayList<>();


    public IntentService() {
        super("IntentService");

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //create file to save the spoofed MACs
        try {
            thePath = File.createTempFile(filename, ".txt", getExternalCacheDir());
        } catch (IOException e) {
            e.printStackTrace();
        }
        uri = Uri.fromFile(thePath);

        try {
            myFile = new PrintWriter(new File(uri.getPath()));
        }
        catch (FileNotFoundException e) {
        }


        //SharedPreferences preferences = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
//        final String inter = preferences.getString("inter", "");
        final String mac = intent.getStringExtra("mac");
        final String inter = intent.getStringExtra("Interface");
        setInterface(inter);



        try {
            PrintWriter pw = new PrintWriter("mac.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


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

            //Log.d("my log grant", "" + checkIfGranted());

            new Timer().scheduleAtFixedRate(new TimerTask() {
                BaseActivity b = new BaseActivity();
                int count = 0;

                @Override
                public void run() {
                  //  Log.d("my log2", b.getStatus() + "");

                    try {
                        if (!b.getStatus() && !LocalizeActivity.readyToSync()) {
                            //Log.d("inter2", inter + "");
                            changeMac(inter);
                            //Log.d("my log", "changing mac");
                        } else {
                               // Log.d("my log", "stting mac back");
                                //setMac(mac, inter);
                        }

                        Log.d("my log3", Utils2.getMACAddress("wlan0"));
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 3000); // changes MAC every 3 seconds


        }
    }
    private void changeMac(String inter) throws IOException, InterruptedException {

        String mac = generateMac();

        //commands to execute
        String[] cmds = {"ip link set " + inter + " address " + mac};


        // execute the commands
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        for (String tmpCmd : cmds) {
            os.writeBytes(tmpCmd + "\n");
        }

        writeToFile(mac);
        readFromFile();
        macList.add(mac);

    }

    public static void setMac(String mac, String inter) throws IOException, InterruptedException {


        //commands to execute
        String[] cmds = {"ip link set " + inter + " address " + mac};

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
            {   // Can't get root !
                // Probably broken pipe exception on trying to write to output stream (os) after su failed, meaning that the device is not rooted
                retval = false;
                Log.d("ROOT", "Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
            }

            return retval;
        }

    private void writeToFile(String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput("config.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            //Log.e("my log", "write failed: " + e.toString());
        }

    }

    private String readFromFile() {

        String ret = "";

        try {
            InputStream inputStream = openFileInput("config.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString).append("\n");
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        //Log.d("file: ", ret);
        return ret;
    }

    private static void setInterface(String inter){
        theInterface = inter;
    }

    public void exportToCsv(String mac) throws IOException {

        for(int i = 0; i < macList.size(); i++){
            myFile.write(macList.get(i) + "\n");
        }

    }

    public static void macsToFile(){
        for(int i = 0; i < macList.size(); i++){
            myFile.write(macList.get(i) + "\n");
        }

        myFile.close();
    }
}







