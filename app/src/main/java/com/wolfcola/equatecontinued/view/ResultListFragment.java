package com.wolfcola.equatecontinued.view;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wolfcola.equatecontinued.Calculator;
import com.wolfcola.equatecontinued.R;
import com.wolfcola.equatecontinued.Result;

import java.util.List;

public class ResultListFragment extends Fragment {
    private CalcViewModel mViewModel;
    private List<Result> mResultArray;
    private RecyclerView mRecyclerView;
    private ResultAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(requireActivity()).get(CalcViewModel.class);
        mResultArray = mViewModel.getCalc().getResultList();

        mRecyclerView = view.findViewById(R.id.result_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new ResultAdapter();
        mRecyclerView.setAdapter(mAdapter);
    }

    /**
     * Update the list and scroll to the bottom
     *
     * @param instantScroll if true, scroll instantly; otherwise use smooth scroll
     */
    public void refresh(boolean instantScroll) {
        if (mAdapter == null) return;
        mAdapter.notifyDataSetChanged();

        int lastPos = mResultArray.size() - 1;
        if (lastPos < 0) return;

        if (instantScroll) {
            mRecyclerView.scrollToPosition(lastPos);
        } else {
            mRecyclerView.smoothScrollToPosition(lastPos);
        }
    }

    private class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ResultViewHolder> {

        @NonNull
        @Override
        public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_result, parent, false);
            return new ResultViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
            Result result = mResultArray.get(position);
            holder.bind(result);
        }

        @Override
        public int getItemCount() {
            return mResultArray.size();
        }

        class ResultViewHolder extends RecyclerView.ViewHolder {
            final DynamicTextView unitDesc;
            final TextView unitTimestamp;
            final TextView queryText;
            final TextView answerText;

            ResultViewHolder(@NonNull View itemView) {
                super(itemView);
                unitDesc = itemView.findViewById(R.id.list_item_result_convertUnitDesc);
                unitTimestamp = itemView.findViewById(R.id.list_item_result_currencyTimestamp);
                queryText = itemView.findViewById(R.id.list_item_result_textPrevQuery);
                answerText = itemView.findViewById(R.id.list_item_result_textPrevAnswer);
            }

            void bind(Result result) {
                unitTimestamp.setVisibility(View.GONE);

                if (result != null && result.containsUnits()) {
                    String text = getResources().getString(R.string.word_Converted) +
                            " " + result.getQueryUnitTextLong() +
                            " " + getResources().getString(R.string.word_to) +
                            " " + result.getAnswerUnitTextLong() + ":";
                    unitDesc.setText(ViewUtils.fromHtml("<i>" + text + "</i>"));
                    unitDesc.setVisibility(View.VISIBLE);

                    String timestamp = result.getTimestamp();
                    if (!timestamp.equals("")) {
                        unitTimestamp.setText(timestamp);
                        unitTimestamp.setVisibility(View.VISIBLE);
                    }
                } else {
                    unitDesc.setVisibility(View.GONE);
                }

                queryText.setText(result.getTextQuery());
                answerText.setText(result.getTextAnswer());

                // Click to load result back into expression
                View.OnClickListener clickListener = view -> {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    Result thisResult = mResultArray.get(pos);
                    Calculator calc = mViewModel.getCalc();

                    String textPassBack = "";
                    int viewID = view.getId();
                    if (viewID == R.id.list_item_result_textPrevQuery)
                        textPassBack = thisResult.getQueryWithoutSep();
                    if (viewID == R.id.list_item_result_textPrevAnswer)
                        textPassBack = thisResult.getAnswerWithoutSep();

                    calc.parseKeyPressed(textPassBack);

                    if (!calc.isUnitSelected() && thisResult.containsUnits()) {
                        int unitPosPassBack;
                        if (viewID == R.id.list_item_result_textPrevQuery)
                            unitPosPassBack = thisResult.getQueryUnitPos();
                        else
                            unitPosPassBack = thisResult.getAnswerUnitPos();

                        mViewModel.selectUnitAtUnitArrayPos(unitPosPassBack, thisResult.getUnitTypeKey());
                    }

                    mViewModel.requestScreenUpdate(false);
                };

                queryText.setOnClickListener(clickListener);
                answerText.setOnClickListener(clickListener);

                // Long click to delete result
                View.OnLongClickListener longClickListener = view -> {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return false;

                    Toast toast = Toast.makeText(view.getContext(), "Result Deleted", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                    mResultArray.remove(pos);
                    notifyItemRemoved(pos);
                    mViewModel.requestScreenUpdate(true);
                    return false;
                };

                queryText.setOnLongClickListener(longClickListener);
                answerText.setOnLongClickListener(longClickListener);
            }
        }
    }
}
