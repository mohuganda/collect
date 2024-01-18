package org.odk.collect.android.formlists.blankformlist

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.odk.collect.android.R
import org.odk.collect.android.databinding.DeleteBlankFormLayoutBinding
import org.odk.collect.androidshared.ui.FragmentFactoryBuilder
import org.odk.collect.androidshared.ui.multiselect.MultiSelectAdapter
import org.odk.collect.androidshared.ui.multiselect.MultiSelectControlsFragment
import org.odk.collect.androidshared.ui.multiselect.MultiSelectItem
import org.odk.collect.androidshared.ui.multiselect.MultiSelectViewModel

class DeleteBlankFormFragment(
    private val viewModelFactory: ViewModelProvider.Factory,
    private val menuHost: MenuHost
) : Fragment() {

    private lateinit var blankFormListViewModel: BlankFormListViewModel
    lateinit var multiSelectViewModel: MultiSelectViewModel<BlankFormListItem>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val viewModelProvider = ViewModelProvider(this, viewModelFactory)
        blankFormListViewModel = viewModelProvider[BlankFormListViewModel::class.java]
        multiSelectViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras
                ): T {
                    return MultiSelectViewModel(
                        blankFormListViewModel.formsToDisplay.map {
                            it.map { blankForm -> MultiSelectItem(blankForm.databaseId, blankForm) }
                        }
                    ) as T
                }
            }
        )[MultiSelectViewModel::class.java] as MultiSelectViewModel<BlankFormListItem>

        childFragmentManager.fragmentFactory = FragmentFactoryBuilder()
            .forClass(MultiSelectControlsFragment::class) {
                MultiSelectControlsFragment(
                    getString(org.odk.collect.strings.R.string.delete_file),
                    multiSelectViewModel
                )
            }
            .build()

        childFragmentManager.setFragmentResultListener(
            MultiSelectControlsFragment.REQUEST_ACTION,
            this
        ) { _, result ->
            val selected = result.getLongArray(MultiSelectControlsFragment.RESULT_SELECTED)!!
            onDeleteSelected(selected)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.delete_blank_form_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = DeleteBlankFormLayoutBinding.bind(view)
        val recyclerView = binding.list
        val adapter = MultiSelectAdapter(multiSelectViewModel) { parent ->
            BlankFormListItemViewHolder(parent).also {
                it.setTrailingView(R.layout.checkbox)
            }
        }

        recyclerView.adapter = adapter

        multiSelectViewModel.getData().observe(viewLifecycleOwner) {
            adapter.data = it

            binding.empty.isVisible = it.isEmpty()
            binding.buttons.isVisible = it.isNotEmpty()
        }

        multiSelectViewModel.getSelected().observe(viewLifecycleOwner) {
            adapter.selected = it
        }

        val blankFormListMenuProvider =
            BlankFormListMenuProvider(requireActivity(), blankFormListViewModel)
        menuHost.addMenuProvider(blankFormListMenuProvider, viewLifecycleOwner, State.RESUMED)
    }

    private fun onDeleteSelected(selected: LongArray) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(org.odk.collect.strings.R.string.delete_file)
            .setMessage(
                getString(
                    org.odk.collect.strings.R.string.delete_confirm,
                    selected.size.toString()
                )
            )
            .setPositiveButton(getString(org.odk.collect.strings.R.string.delete_yes)) { _, _ ->
                blankFormListViewModel.deleteForms(*selected)
                multiSelectViewModel.unselectAll()
            }
            .setNegativeButton(getString(org.odk.collect.strings.R.string.delete_no), null)
            .show()
    }
}
