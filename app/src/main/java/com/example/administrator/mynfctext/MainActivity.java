package com.example.administrator.mynfctext;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.os.Environment.getExternalStorageDirectory;

/**
 * Created by Jeff on 16-10-25
 * 467407802@qq.com
 */
public class MainActivity extends Activity implements View.OnClickListener {
    private IsoDep myTag;
    private TextView tv_log;
    private EditText tv_editText, et_filePath;
    private Button tv_sure;
    private NfcAdapter mAdapter = null;
    private PendingIntent mPendingIntent;
    private String[][] mTechLists;
    private IntentFilter[] mFilters;
    private RadioButton send, choise;
    private ImageView iv_dialog;
    private File file;
    private List<String> name = new ArrayList<>();
    //Folder for instruction file
    private final String fileCommand = "/fileCommand/";
    private HexadecimalKbd Hexkbd;
    private ActionBar actionBar;
    private String str = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        actionBar = getActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setIcon(R.drawable.nfclogo);
        actionBar.setTitle("CardTool");
        setContentView(R.layout.activity_main);
        Hexkbd = new HexadecimalKbd(this, R.id.keyboardview, R.xml.hexkbd);
        Hexkbd.registerEditText(R.id.tv_editText);
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        mFilters = new IntentFilter[]{ndef};
        mTechLists = new String[][]{new String[]{IsoDep.class.getName()}};
        tv_log = (TextView) findViewById(R.id.tv_log);
        tv_editText = (EditText) findViewById(R.id.tv_editText);
        tv_editText.setOnClickListener(this);

        send = (RadioButton) findViewById(R.id.send);
        choise = (RadioButton) findViewById(R.id.choise);
        send.setOnClickListener(this);
        tv_sure = (Button) findViewById(R.id.tv_sure);
        et_filePath = (EditText) findViewById(R.id.et_filePath);
        et_filePath.setOnClickListener(this);
        iv_dialog = (ImageView) findViewById(R.id.iv_dialog);
        iv_dialog.setOnClickListener(this);
        tv_sure.setOnClickListener(this);
        tv_sure.setEnabled(false);
        send.setEnabled(false);
        choise.setEnabled(false);
        onNewIntent(getIntent());
        getTxtFile();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                saveLog();
                return true;
            case R.id.clear:
                tv_log.setText("");
                return true;
            case R.id.share:
                shareText("分享", "我的主题", tv_log.getText().toString());
                return true;
            case R.id.about:
                showAboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAboutDialog() {
        final AlertDialog.Builder aboutDialog = new AlertDialog.Builder(this);
        aboutDialog.setTitle("About us:");
        aboutDialog.setView(R.layout.layout_dialog);
        aboutDialog.setNegativeButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        aboutDialog.show();
    }

    /**
     * Share text content
     *
     * @param dlgTitle Share dialog title
     * @param subject  theme
     * @param content  Share content (text)
     */
    private void shareText(String dlgTitle, String subject, String content) {
        if (content == null || "".equals(content)) {
            Toast.makeText(this, "Sharing content is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        if (subject != null && !"".equals(subject)) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        }

        intent.putExtra(Intent.EXTRA_TEXT, content);

        // Set pop-up box title
        if (dlgTitle != null && !"".equals(dlgTitle)) { // Custom title
            startActivity(Intent.createChooser(intent, dlgTitle));
        } else { // System default header
            startActivity(intent);
        }
    }

    /**
     * Get all the files in this folder under /fileCommand/
     */
    private void getTxtFile() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            File path = getExternalStorageDirectory();
            String newFile = path.getPath() + fileCommand;
            File file = new File(newFile);
            if (!file.exists()) {
                file.mkdir();
            }
            File[] files = file.listFiles();
            getFileName(files);
        }
    }

    String datas[];

    /**
     * @param files File array
     *              Get the.Txt end of the file
     */
    private void getFileName(File[] files) {
        name.clear();
        if (files != null) {// First to determine whether the directory is empty, otherwise it will be reported to the null pointer
            for (File file : files) {
                if (!file.isDirectory()) {
                    String fileName = file.getName();
                    if (fileName.endsWith(".txt")) {
                        name.add(fileName);
                    }
                }
            }
            datas = new String[name.size()];
            for (int i = 0; i < name.size(); i++) {
                datas[i] = name.get(i) + "";
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);
    }

    String szATR = null;

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
        }
        if (mAdapter == null) {
            Toast.makeText(getApplicationContext(), "PLEASE TAP CARD", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        byteAPDU = null;
        respAPDU = null;
        mAdapter.disableForegroundDispatch(this);
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            final Tag t = (Tag) tag;
            myTag = IsoDep.get(t);
            if (!myTag.isConnected()) {
                try {
                    myTag.connect();
                    myTag.setTimeout(5000);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
            if (myTag.isConnected()) {
                tv_sure.setEnabled(true);
                send.setEnabled(true);
                choise.setEnabled(true);
                Toast.makeText(getApplicationContext(), "Scan to card!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static String getHexString(byte[] data) throws Exception {
        String szDataStr = "";
        for (int ii = 0; ii < data.length; ii++) {
            szDataStr += String.format("%02X ", data[ii] & 0xFF);
        }
        return szDataStr;
    }

    /**
     * save log
     */
    private void saveLog() {
        if (tv_log.getText().toString().equals("")) {
            Toast.makeText(this, "Log for empty save failed", Toast.LENGTH_SHORT).show();
            return;
        }
        String pathName1 = Environment.getExternalStorageDirectory() + fileCommand;
        String fileName1 = getTransDate() + ".txt";
        File path1 = new File(pathName1);
        File file1 = new File(pathName1 + fileName1);
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            try {
                if (!path1.exists()) {
                    path1.mkdir();
                }
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(file1);
                fos.write(tv_log.getText().toString().getBytes());
                fos.close();
                Toast.makeText(MainActivity.this, "Save to" + file1.getPath() + "path success",
                        Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "fail to write to file",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // At this point SDcard does not exist or can not be read and write operations
            Toast.makeText(MainActivity.this,
                    "At this point SDcard does not exist or " +
                            "can not be read and write operations", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_sure:
                file = new File(getExternalStorageDirectory() + fileCommand,
                        et_filePath.getText().toString());
                if (myTag.equals(null) || myTag.equals("")) {
                    vShowErrorVaules();
                    return;
                } else if (myTag.isConnected()) {
                    if (!bSendAPDU()) {
                        vShowErrorVaules();
                    }
                }
                break;
            case R.id.iv_dialog:
                choise.setChecked(true);
                showDial();
                break;
            case R.id.tv_editText:
                send.setChecked(true);
                Hexkbd.showCustomKeyboard(v);
                break;
        }
    }

    /**
     * Gets the current time stamp as the name of the currently generated log file!
     */
    private String getTransDate() {
        Date now = new Date();
        SimpleDateFormat spf = new SimpleDateFormat("yyyyMMddHHmmss");
        return spf.format(now);
    }

    private void vShowErrorVaules() {
        Context context = getApplicationContext();
        CharSequence text = "C-APDU values ERROR";
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    static byte[] byteAPDU = null;
    static byte[] respAPDU = null;

    private boolean bSendAPDU() {
        if (send.isChecked()) {
            if (tv_editText.getText().toString().equals("")) {
                Toast.makeText(this, "Please enter a query!", Toast.LENGTH_SHORT).show();
            } else {

                String StringAPDU = tv_editText.getText().toString();
                byteAPDU = atohex(StringAPDU);
                transceives(byteAPDU);
            }
        } else if (choise.isChecked()) {
            if (file != null && !et_filePath.getText().toString().equals("")) {
                printFile(file);
            } else {
                Toast.makeText(this, "Please enter a query!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please select a command to send the method", Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (Hexkbd.isCustomKeyboardVisible())
            Hexkbd.hideCustomKeyboard();
        else
            this.finish();
    }

    public void HideKbd() {
        if (Hexkbd.isCustomKeyboardVisible())
            Hexkbd.hideCustomKeyboard();
    }

    private void showDial() {
        if (datas.equals(null)) {
            Toast.makeText(this, "Select the root directory /fileCommand/ under the command file", Toast.LENGTH_LONG).show();
        }
        getTxtFile();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select command file:");
        builder.setItems(datas, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                HideKbd();
                dialog.dismiss();
                et_filePath.setText(datas[which]);
            }
        });
        builder.show();
    }

    private static byte[] atohex(String data) {
        String hexchars = "0123456789abcdef";

        data = data.replaceAll(" ", "").toLowerCase();
        if (data == null) {
            return null;
        }
        byte[] hex = new byte[data.length() / 2];

        for (int ii = 0; ii < data.length(); ii += 2) {
            int i1 = hexchars.indexOf(data.charAt(ii));
            int i2 = hexchars.indexOf(data.charAt(ii + 1));
            hex[ii / 2] = (byte) ((i1 << 4) | i2);
        }
        return hex;
    }

    private byte[] transceives(byte[] data) {
        byte[] ra = null;
        String RESP = "";
        try {
            RESP = getHexString(data).toUpperCase();
            print("IFD:" + getHexString(data));
        } catch (Exception e1) {
        }

        try {
            ra = myTag.transceive(data);
        } catch (IOException e) {
            print("************************************");
            print("         NO CARD RESPONSE");
            print("************************************");
        }
        try {
            String cc = RESP.replace(" ", "").substring(0, 4);
            String a = getHexString(ra).replace(" ", "");
            print("ICC:" + getHexString(ra));
            if (cc.equals("00B0")) {
                String carNo = a.substring(0, a.length() - 4);
                print("CardNumber:" + carNo);
            }
            if (cc.equals("805C")) {
                String blance16 = a.substring(4, a.length() - 4);
                String carBlance = new BigInteger(blance16, 16) + "";
                print("Balance:" + getConversion(carBlance));
            }
            if (cc.equals("00B2")) {
                String date = getHexString(ra).substring(
                        getHexString(ra).length() - 27, getHexString(ra).length() - 7).replace(" ", "-");
                String b = a.substring(14, 18);
                String type = a.substring(18, 20);
                if (type.equals("02")) {
                    String recharge = "" + new BigInteger(b, 16);
                    print("Recharge:" + getConversion(recharge) + "    Date:" + date);
                } else if (type.equals("06")) {
                    String consumption = "" + new BigInteger(b, 16);
                    print("Consumption:" + getConversion(consumption) + "    Date:" + date);
                } else if (type.equals("09")) {
                    String consumption = "" + new BigInteger(b, 16);
                    print("Consumption:" + getConversion(consumption) + "    Date:" + date);
                }
            }

            print("*********************");
        } catch (Exception e1) {
        }

        return (ra);
    }

    private static String getConversion(String data) {
        double intData = Double.parseDouble(data);
        String newData = intData / 100 + "";
        return newData;

    }

    private void print(String s) {
        tv_log.append(s);
        tv_log.append("\r\n");
        return;
    }

    /**
     * Cycle read single line record
     *
     * @param route File directory
     */
    private void printFile(File route) {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            InputStream is = null;
            FileInputStream fis = null;
            InputStreamReader isr = null;
            BufferedReader br = null;
            try {
                fis = new FileInputStream(route);// FileInputStream
                isr = new InputStreamReader(fis);
                br = new BufferedReader(isr);
                while ((str = br.readLine()) != null) {
                    byteAPDU = atohex(str);
                    transceives(byteAPDU);
                    String state = getHexString(myTag.transceive(byteAPDU)).substring(getHexString(myTag.transceive(byteAPDU)).length() - 6, getHexString(myTag.transceive(byteAPDU)).length());
                    if (!state.equals("90 00 ")) {
                        Toast.makeText(this, "Query error", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (FileNotFoundException e) {
                Toast.makeText(MainActivity.this.getApplicationContext(), "Could not find the specified file", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "fail to read file", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    br.close();
                    isr.close();
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}
