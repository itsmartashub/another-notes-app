/*
 * Copyright 2020 Nicolas Maltais
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.maltaisn.notes.ui.sync.signup

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.maltaisn.notes.databinding.FragmentSyncSignUpBinding
import com.maltaisn.notes.hideKeyboard
import com.maltaisn.notes.setCustomEndIconOnClickListener
import com.maltaisn.notes.ui.EventObserver
import com.maltaisn.notes.ui.common.ViewModelFragment
import com.maltaisn.notes.ui.sync.SyncFragment
import com.maltaisn.notes.ui.sync.SyncPage
import com.maltaisn.notes.ui.sync.signup.SyncSignUpViewModel.FieldError


class SyncSignUpFragment : ViewModelFragment() {

    private val viewModel: SyncSignUpViewModel by viewModels { viewModelFactory }

    private var _binding: FragmentSyncSignUpBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSyncSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailLayout = binding.emailEdtLayout
        val passwordLayout = binding.passwordEdtLayout
        val passwordConfirmLayout = binding.passwordConfirmEdtLayout
        val emailEdt = binding.emailEdt
        val passwordEdt = binding.passwordEdt
        val passwordConfirmEdt = binding.passwordConfirmEdt


        binding.signInBtn.setOnClickListener {
            viewModel.goToPage(SyncPage.SIGN_IN)
        }
        binding.signUpBtn.setOnClickListener {
            view.hideKeyboard()
            viewModel.signUp()
        }

        passwordLayout.setCustomEndIconOnClickListener {
            val passwordHidden = passwordEdt.transformationMethod is PasswordTransformationMethod
            viewModel.setPasswordConfirmEnabled(passwordHidden)
        }

        emailEdt.doAfterTextChanged {
            viewModel.setEnteredEmail(it?.toString() ?: "")
        }
        passwordEdt.doAfterTextChanged {
            viewModel.setEnteredPassword(it?.toString() ?: "")
        }
        passwordConfirmEdt.doAfterTextChanged {
            viewModel.setEnteredPasswordConfirm(it?.toString() ?: "")
        }

        // Observers
        viewModel.changePageEvent.observe(viewLifecycleOwner, EventObserver { page ->
            (parentFragment as? SyncFragment)?.goToPage(page)
        })

        viewModel.messageEvent.observe(viewLifecycleOwner, EventObserver { messageId ->
            Snackbar.make(view, messageId, Snackbar.LENGTH_SHORT).show()
        })

        viewModel.fieldError.observe(viewLifecycleOwner, Observer { error ->
            val txvLayouts = listOf(emailLayout, passwordLayout, passwordConfirmLayout)
            val errorLayout = when (error?.location) {
                null -> null
                FieldError.Location.EMAIL -> emailLayout
                FieldError.Location.PASSWORD -> passwordLayout
                FieldError.Location.PASSWORD_CONFIRM -> passwordConfirmLayout
            }
            for (layout in txvLayouts) {
                if (layout === errorLayout) {
                    layout.error = getString(error!!.messageId, *error.args)
                } else {
                    layout.error = null
                    layout.isErrorEnabled = false
                }
            }
        })

        viewModel.passwordConfirmEnabled.observe(viewLifecycleOwner, Observer { enabled ->
            passwordConfirmLayout.isVisible = enabled
            if (!enabled) {
                passwordConfirmEdt.text = null
            }
        })

        viewModel.clearFieldsEvent.observe(viewLifecycleOwner, EventObserver {
            emailEdt.text = null
            passwordEdt.text = null
            passwordConfirmEdt.text = null
            emailEdt.clearFocus()
            passwordEdt.clearFocus()
            passwordConfirmEdt.clearFocus()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
