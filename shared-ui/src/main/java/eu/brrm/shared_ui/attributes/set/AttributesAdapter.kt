package eu.brrm.shared_ui.attributes.set

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eu.brrm.shared_ui.attributes.CustomAttribute
import eu.brrm.shared_ui.databinding.RowAttributeBinding


private val difUtil = object : DiffUtil.ItemCallback<CustomAttribute>() {
    override fun areItemsTheSame(
        oldItem: CustomAttribute,
        newItem: CustomAttribute
    ): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: CustomAttribute,
        newItem: CustomAttribute
    ): Boolean {
        val oldString = oldItem.asString()
        val newString = newItem.asString()

        return oldItem.name == newItem.name && oldString == newString && oldItem.type == newItem.type
    }
}

class AttributesAdapter(private val onDeleteAttribute: (CustomAttribute) -> Unit) :
    ListAdapter<CustomAttribute, AttributesViewHolder>(difUtil) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AttributesViewHolder {
        return AttributesViewHolder.create(parent)
    }

    override fun onBindViewHolder(
        holder: AttributesViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
        holder.binding.ibDelete.setOnClickListener {
            onDeleteAttribute(getItem(position))
        }
    }
}

class AttributesViewHolder(val binding: RowAttributeBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: CustomAttribute) {
        binding.tvName.text = item.name
        binding.tvType.text = item.type.typeName
        binding.tvValue.text = item.asString()
    }

    companion object {
        fun create(parent: ViewGroup): AttributesViewHolder {
            val binding =
                RowAttributeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return AttributesViewHolder(binding)
        }
    }
}