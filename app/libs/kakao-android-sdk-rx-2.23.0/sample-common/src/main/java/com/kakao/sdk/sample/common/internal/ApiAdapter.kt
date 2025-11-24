/*
  Copyright 2019 Kakao Corp.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.kakao.sdk.sample.common.internal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kakao.sdk.sample.common.databinding.ItemApiBinding
import com.kakao.sdk.sample.common.databinding.ItemHeaderBinding
import java.lang.ClassCastException

class ApiAdapter(private val apis: List<Item>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                HeaderHolder(ItemHeaderBinding.inflate(inflater, parent, false))
            }
            1 -> {
                ApiHolder(ItemApiBinding.inflate(inflater, parent, false))
            }
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun getItemCount(): Int = apis.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = apis[position]
        when (holder) {
            is HeaderHolder -> {
                holder.bind(item as Item.Header)
            }
            is ApiHolder -> {
                holder.bind(item as Item.ApiItem)
            }
        }
    }

    override fun getItemViewType(position: Int): Int = when (apis[position]) {
        is Item.Header -> 0
        is Item.ApiItem -> 1
    }

    class HeaderHolder(private val binding: ItemHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item.Header) {
            binding.headerTitle = item.title
        }
    }

    class ApiHolder(private val binding: ItemApiBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item.ApiItem) {
            binding.apiLabel = item.label
            binding.root.setOnClickListener { item.apiFunction() }
        }
    }

    sealed class Item {
        data class ApiItem(val label: String, val apiFunction: () -> Unit) : Item()
        data class Header(val title: String) : Item()
    }

}