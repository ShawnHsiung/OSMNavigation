package shawn.thesis.osmnavigation;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.math.BigInteger;


public class CommandSettingFragment extends Fragment implements MapStateManager {

    EditText LeftCommand;
    EditText RightCommand;
    EditText ForwardCommand;
    EditText StopCommand;
    Button SaveSetting;
    Button SetLeft;
    Button SetRight;
    Button SetForward;
    Button SetStop;
    Button HexaBinaryButton;
    boolean TextSavedAsBinary = true;

    String DefaultCommandBinary = "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";

    private SharedPreferences mPrefs;

    private OnFragmentCommandListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_command, container, false);

        LeftCommand = (EditText) view.findViewById(R.id.left_text);
        RightCommand = (EditText) view.findViewById(R.id.right_text);
        ForwardCommand = (EditText) view.findViewById(R.id.forward_text);
        StopCommand = (EditText) view.findViewById(R.id.stop_text);

        SaveSetting = (Button) view.findViewById(R.id.save_setting);
        SetLeft = (Button) view.findViewById(R.id.set_setting_left);
        SetRight = (Button) view.findViewById(R.id.set_setting_right);
        SetForward = (Button) view.findViewById(R.id.set_setting_forward);
        SetStop = (Button) view.findViewById(R.id.set_setting_stop);

        mPrefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        LeftCommand.setText(mPrefs.getString("PREFS_COMMAND_LEFT", DefaultCommandBinary));
        RightCommand.setText(mPrefs.getString("PREFS_COMMAND_RIGHT", DefaultCommandBinary));
        ForwardCommand.setText(mPrefs.getString("PREFS_COMMAND_FORWARD", DefaultCommandBinary));
        StopCommand.setText(mPrefs.getString("PREFS_COMMAND_STOP", DefaultCommandBinary));
        TextSavedAsBinary = mPrefs.getBoolean("PREFS_COMMAND_IS_BINARY",true);

        SaveSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putString("PREFS_COMMAND_LEFT", LeftCommand.getText().toString());
                editor.putString("PREFS_COMMAND_RIGHT", RightCommand.getText().toString());
                editor.putString("PREFS_COMMAND_FORWARD", ForwardCommand.getText().toString());
                editor.putString("PREFS_COMMAND_STOP", StopCommand.getText().toString());
                editor.putBoolean("PREFS_COMMAND_IS_BINARY", TextSavedAsBinary);
                editor.commit();
                Toast.makeText(getActivity(), "Data is saved successfully ", Toast.LENGTH_LONG).show();
            }
        });
        SetLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        SetRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
        SetForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
        SetStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        return view;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((MainActivity) getActivity()).setActionBarTitle("Command Setting");
//        ((MainActivity) getActivity()).getSupportActionBar().hide();
    }

    void ConvertAllToBinary(){
        if(TextSavedAsBinary ==false){
            LeftCommand.setText(HexToBinary(LeftCommand.getText().toString()));
            RightCommand.setText(HexToBinary(RightCommand.getText().toString()));
            ForwardCommand.setText(HexToBinary(ForwardCommand.getText().toString()));
            StopCommand.setText(HexToBinary(StopCommand.getText().toString()));
            TextSavedAsBinary =true;
            HexaBinaryButton.setText("Hex");
            HexaBinaryButton.getBackground().setColorFilter(Color.argb(255, 150, 50, 150), PorterDuff.Mode.DARKEN);
        }
    }

    void ConvertAllToHex(){
        if(TextSavedAsBinary ==true){
            LeftCommand.setText(BinaryToHex(LeftCommand.getText().toString()));
            RightCommand.setText(BinaryToHex(RightCommand.getText().toString()));
            ForwardCommand.setText(BinaryToHex(ForwardCommand.getText().toString()));
            StopCommand.setText(BinaryToHex(StopCommand.getText().toString()));
            TextSavedAsBinary =false;
            HexaBinaryButton.setText("Bin");
            HexaBinaryButton.getBackground().setColorFilter(Color.argb(255, 50, 200, 150), PorterDuff.Mode.DARKEN);
        }
    }


    String HexToBinary(String HexValue){
        String BinaryValue="";
        for(int i =0; i<40; i++)
        {
            String bin =  new BigInteger(HexValue.substring(0 + i, 1 + i), 16).toString(2);
            int inb = Integer.parseInt(bin);
            BinaryValue = BinaryValue+ String.format("%04d", inb);
        }
        return BinaryValue;
    }

    public static String BinaryToHex(String BinaryValue){
        String HexValue="";
        for(int i =0; i<40; i++)
        {
            HexValue=HexValue+String.valueOf(Integer.toHexString(Integer.parseInt(BinaryValue.substring(0 + 4 * i, 4 + 4 * i), 2)));
        }
        return HexValue;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentCommandListener) {
            mListener = (OnFragmentCommandListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentCommandListener {
        void onCommandSettingFragment();
    }
}
