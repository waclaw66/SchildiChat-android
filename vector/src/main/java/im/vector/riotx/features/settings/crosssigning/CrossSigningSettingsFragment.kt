/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.riotx.features.settings.crosssigning

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.internal.crypto.model.rest.UserPasswordAuth
import im.vector.riotx.R
import im.vector.riotx.core.dialogs.PromptPasswordDialog
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.extensions.observeEvent
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.platform.VectorBaseFragment
import kotlinx.android.synthetic.main.fragment_generic_recycler.*
import javax.inject.Inject

class CrossSigningSettingsFragment @Inject constructor(
        private val epoxyController: CrossSigningEpoxyController,
        val viewModelFactory: CrossSigningSettingsViewModel.Factory
) : VectorBaseFragment(), CrossSigningEpoxyController.InteractionListener {

    override fun getLayoutResId() = R.layout.fragment_generic_recycler

    private val viewModel: CrossSigningSettingsViewModel by fragmentViewModel()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.requestLiveData.observeEvent(this) {
            when (it) {
                is Fail    -> {
                    AlertDialog.Builder(requireContext())
                            .setTitle(R.string.dialog_title_error)
                            .setMessage(it.error.message)
                            .setPositiveButton(R.string.ok, null)
                            .show()
                }
                is Success -> {
                    when (val action = it.invoke()) {
                        is CrossSigningAction.RequestPasswordAuth -> {
                            requestPassword(action.sessionId)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(R.string.encryption_information_cross_signing_state)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        epoxyController.setData(state)
    }

    private fun setupRecyclerView() {
        recyclerView.configureWith(epoxyController, hasFixedSize = false, disableItemAnimation = true)
        epoxyController.interactionListener = this
    }

    override fun onDestroyView() {
        recyclerView.cleanup()
        epoxyController.interactionListener = null
        super.onDestroyView()
    }

    private fun requestPassword(sessionId: String) {
        PromptPasswordDialog().show(requireActivity()) { password ->
            // TODO sessionId should never get out the ViewModel
            viewModel.handle(CrossSigningAction.InitializeCrossSigning(UserPasswordAuth(
                    session = sessionId,
                    password = password
            )))
        }
    }

    override fun onInitializeCrossSigningKeys() {
        viewModel.handle(CrossSigningAction.InitializeCrossSigning())
    }

    override fun onResetCrossSigningKeys() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_title_confirmation)
                .setMessage(R.string.are_you_sure)
                .setPositiveButton(R.string.ok) { _, _ ->
                    viewModel.handle(CrossSigningAction.InitializeCrossSigning())
                }
                .show()
    }
}
