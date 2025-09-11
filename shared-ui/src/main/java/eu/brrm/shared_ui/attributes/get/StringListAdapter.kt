package eu.brrm.shared_ui.attributes.get

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eu.brrm.shared_ui.databinding.RowSimpleTextItemBinding

private val difUtil = object : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: String,
        newItem: String
    ): Boolean {
        return oldItem == newItem
    }
}

class StringListAdapter(private val onDelete: (String) -> Unit) :
    ListAdapter<String, TextViewHolder>(difUtil) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TextViewHolder {
        return TextViewHolder.create(parent)
    }

    override fun onBindViewHolder(
        holder: TextViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
        holder.binding.btnDelete.setOnClickListener {
            onDelete(getItem(position))
        }
    }
}

class TextViewHolder(val binding: RowSimpleTextItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: String) {
        binding.tvTitle.text = item
    }

    companion object {
        fun create(parent: ViewGroup): TextViewHolder {
            val binding = RowSimpleTextItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return TextViewHolder(binding)
        }
    }
}