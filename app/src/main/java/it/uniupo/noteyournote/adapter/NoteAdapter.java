package it.uniupo.noteyournote.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import it.uniupo.noteyournote.R;
import it.uniupo.noteyournote.model.Note;
import it.uniupo.noteyournote.util.PhotoUtil;

public class NoteAdapter extends ArrayAdapter<Note> {

    private Context mContext;
    private List<Note> mDataset;

    public NoteAdapter(Context context, int resource, List<Note> dataset) {
        super(context, resource);
        mContext = context;
        mDataset = dataset;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext)
                    .inflate(R.layout.note_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Note note = mDataset.get(position);
        holder.title.setText(note.getTitle());
        if (!TextUtils.isEmpty(note.getDescription())) {
            holder.description.setVisibility(View.VISIBLE);
            holder.description.setText(note.getDescription());
        }
        if (note.getImage() != null) {
            holder.image.setVisibility(View.VISIBLE);
            Glide
                    .with(mContext)
                    .load(PhotoUtil.getBitmapFromBytes(note.getImage().toBytes()))
                    .into(holder.image);
        }

        return convertView;
    }

    @Override
    public int getCount() {
        return mDataset.size();
    }

    public class ViewHolder {
        TextView title;
        TextView description;
        ImageView image;

        ViewHolder(View view) {
            title = view.findViewById(R.id.note_title);
            description = view.findViewById(R.id.note_description);
            image = view.findViewById(R.id.note_image);
        }
    }
}
