package com.wolfcola.equatecontinued.view;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;

import com.wolfcola.equatecontinued.Calculator;
import com.wolfcola.equatecontinued.R;
import com.wolfcola.equatecontinued.Result;

import java.util.List;

public class ResultListFragment extends ListFragment {
    //this is for communication with the parent activity
    private UnitSelectListener mCallback;
    private List<Result> mResultArray;

    /**
     * Override onAttach to check that this fragment implements
     * the @{@link UnitSelectListener} interface.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        //Make sure container implements callback interface; else, throw exception
        try {
            mCallback = (UnitSelectListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement UnitSelectListener");
        }
    }

    /**
     * Called when the fragment's activity has been created and this
     * fragment's view hierarchy instantiated.  It can be used to do final
     * initialization once these pieces are in place, such as retrieving
     * views or restoring state.  It is also useful for fragments that use
     * {@link #setRetainInstance(boolean)} to retain their instance,
     * as this callback tells the fragment when it is fully associated with
     * the new activity instance.  This is called after {@link #onCreateView}
     * and before {@link #onViewStateRestored(Bundle)}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mResultArray = Calculator.getCalculator(getActivity()).getResultList();

        ResultAdapter adapter = new ResultAdapter(mResultArray);
        setListAdapter(adapter);
    }

    /**
     * Update the listView and scroll to the bottom
     *
     * @param instantScroll if false, use animated scroll to bottom,
     *                      otherwise use scroll instantly
     */
    public void refresh(boolean instantScroll) {
        if (getListAdapter() == null) return;
        //notify the adapter that the listview needs to be updated
        ((ResultAdapter) getListAdapter()).notifyDataSetChanged();

        //scroll to the bottom of the list
        if (instantScroll) {
            //post a runnable for setSelection otherwise it won't be called

            getListView().post(new Runnable() {
                @Override
                public void run() {
                    //attempt to fix bug:
                    if (getListAdapter() == null) return;
                    //	try {
                    getListView().setSelection(getListAdapter().getCount() - 1);
                    //	} catch (IllegalStateException e) {
                    //	}
                }
            });

        } else
            getListView().smoothScrollToPosition(getListAdapter().getCount() - 1);
    }

    /**
     * Container Activity must implement this interface
     */
    interface UnitSelectListener {
        void updateScreen(boolean updateResult);

        void selectUnitAtUnitArrayPos(int unitPos, String unitTypeKey);
    }

    private class ResultAdapter extends ArrayAdapter<Result> {
        ResultAdapter(List<Result> prevTest) {
            super(getActivity(), 0, prevTest);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView,
                            @NonNull ViewGroup parent) {
            // If we weren't given a view, inflate one
            if (convertView == null)
                convertView = getActivity().getLayoutInflater().
                        inflate(R.layout.list_item_result, parent, false);

            // Configure the view for this result
            Result result = getItem(position);

            DynamicTextView textViewUnitDesc = (DynamicTextView) convertView.
                    findViewById(R.id.list_item_result_convertUnitDesc);
            TextView textViewUnitTimestamp = (TextView) convertView.
                    findViewById(R.id.list_item_result_currencyTimestamp);
            textViewUnitTimestamp.setVisibility(View.GONE);
            if (result != null && result.containsUnits()) {
                String text = getResources().getString(R.string.word_Converted) +
                        " " + result.getQueryUnitTextLong() +
                        " " + getResources().getString(R.string.word_to) +
                        " " + result.getAnswerUnitTextLong() + ":";
                textViewUnitDesc.setText(ViewUtils.fromHtml("<i>" + text + "</i>"));
                //ListView reuses old textViewUnitDesc sometimes; make sure old one isn't still invisible
                textViewUnitDesc.setVisibility(View.VISIBLE);

                //see if the result was dynamic and therefore has a timestamp to display
                String timestamp = result.getTimestamp();
                if (!timestamp.equals("")) {
                    textViewUnitTimestamp.setText(timestamp);
                    textViewUnitTimestamp.setVisibility(View.VISIBLE);
                }
            } else {
                textViewUnitDesc.setVisibility(View.GONE);
            }

            TextView textViewQuery = (TextView) convertView.
                    findViewById(R.id.list_item_result_textPrevQuery);
            setUpResultTextView(textViewQuery, result.getTextQuery());

            TextView textViewAnswer = (TextView) convertView.
                    findViewById(R.id.list_item_result_textPrevAnswer);
            setUpResultTextView(textViewAnswer, result.getTextAnswer());

            return convertView;
        }

        /**
         * Helper function to reduce repeated code. Sets up the query and answer textViews
         *
         * @param textView the TextView to setup
         * @param text     the previous query or answer String
         */
        private void setUpResultTextView(TextView textView, String text) {
            //mNameTextView.setClickable(true);
			/*
			//want to superscript text after a "^" character
			String [] splitArray = text.split("\\^");
			//only upper-case text if it exists
			if(splitArray.length>1){
				//cut out the "^"
				SpannableString spanText = new SpannableString(splitArray[0] + splitArray[1]);
				//superscript the portion after the "^"
				spanText.setSpan(new SuperscriptSpan(), splitArray[0].length(), spanText.length(), 0);
				mNameTextView.setText(spanText, BufferType.SPANNABLE);
			}

			//otherwise just set it normally
			else
			 */
            textView.setText(text);

            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //error case
                    if (view == null) {
                        Toast toast = Toast.makeText(getActivity(), "ERROR: onClick parameter view is null", Toast.LENGTH_LONG);
                        toast.show();
                        return;
                    }

                    //get the listView position of this answer/query
                    int position = getListView().getPositionForView((View) view.getParent());
                    //grab the calc
                    Calculator calc = Calculator.getCalculator(getActivity());
                    //grab the associated previous expression
                    Result thisResult = mResultArray.get(position);


                    //get text to pass back to calc
                    String textPassBack = "";
                    int viewID = view.getId();
                    if (viewID == R.id.list_item_result_textPrevQuery)
                        textPassBack = thisResult.getQueryWithoutSep();
                    if (viewID == R.id.list_item_result_textPrevAnswer)
                        textPassBack = thisResult.getAnswerWithoutSep();

                    calc.parseKeyPressed(textPassBack);

                    //if unit not selected in calc, and result has unit, set that unit
                    if (!calc.isUnitSelected() && thisResult.containsUnits()) {
                        int unitPosPassBack;
                        if (viewID == R.id.list_item_result_textPrevQuery)
                            unitPosPassBack = thisResult.getQueryUnitPos();
                        else
                            unitPosPassBack = thisResult.getAnswerUnitPos();

                        //if the selection was a success (and we weren't in the wrong unitType), then set the color
                        //int selectedUnitPos = calc.getCurrUnitType().selectUnit(unitPassBack);
                        //if(selectedUnitPos != -1)
                        mCallback.selectUnitAtUnitArrayPos(unitPosPassBack, thisResult.getUnitTypeKey());
                    }

                    mCallback.updateScreen(false);
                }
            });

            textView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Toast toast = Toast.makeText(view.getContext(), "Result Deleted", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                    //get the listView position of this answer/query
                    int position = getListView().getPositionForView((View) view.getParent());
                    //delete associated previous expression
                    mResultArray.remove(position);
                    mCallback.updateScreen(true);


                    return false;
                }
            });
        }
    }
}
