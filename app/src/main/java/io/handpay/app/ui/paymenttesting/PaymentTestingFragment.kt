package io.handpay.app.ui.paymenttesting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.handpay.app.R
import io.handpay.app.databinding.FragmentPaymentTestingBinding
import io.nearpay.sdk.Environments
import io.nearpay.sdk.NearPay
import io.nearpay.sdk.utils.PaymentText
import io.nearpay.sdk.utils.SecondDisplayConfiguration
import io.nearpay.sdk.utils.enums.AuthenticationData
import io.nearpay.sdk.utils.enums.AuthenticationData.*
import io.nearpay.sdk.utils.enums.NetworkConfiguration
import io.nearpay.sdk.utils.enums.PinPosition
import io.nearpay.sdk.utils.enums.PurchaseFailure
import io.nearpay.sdk.utils.enums.SetupFailure
import io.nearpay.sdk.utils.enums.SupportSecondDisplay
import io.nearpay.sdk.utils.enums.TransactionData
import io.nearpay.sdk.utils.enums.UIPosition
import io.nearpay.sdk.utils.listeners.PurchaseListener
import io.nearpay.sdk.utils.listeners.SetupListener
import java.util.Locale
import java.util.UUID

class PaymentTestingFragment : Fragment() {

    private var _binding: FragmentPaymentTestingBinding? = null
    private val binding get() = _binding!!
    private var nearPay: NearPay? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentTestingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupNearPay()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnStartPayment.setOnClickListener {
            startPaymentRequest()
        }
        
        binding.btnSetupNearPay.setOnClickListener {
            setupNearPay()
        }
    }

    private fun setupNearPay() {
        try {
            nearPay = NearPay.Builder()
                .context(requireContext())
                .authenticationData(AuthenticationData.UserEnter)
                .environment(Environments.SANDBOX)
                .locale(Locale.getDefault())
                .supportSecondDisplay(SupportSecondDisplay.Enable)
                .secondDisplayConfiguration(
                    SecondDisplayConfiguration(
                        UIPosition.CENTER,
                        PinPosition.SECONDARY_SCREEN
                    )
                )
                .networkConfiguration(NetworkConfiguration.DEFAULT)
                .uiPosition(UIPosition.CENTER_BOTTOM)
                .paymentText(PaymentText("يرجى تمرير البطاقة", "please tap your card"))
                .loadingUi(true)
                .build()

            nearPay?.setup(object : SetupListener {
                override fun onSetupCompleted() {
                    requireActivity().runOnUiThread {
                        binding.tvStatus.text = "Near Pay Setup Completed"
                        binding.btnStartPayment.isEnabled = true
                        Toast.makeText(requireContext(), "Near Pay setup completed successfully", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onSetupFailed(setupFailure: SetupFailure) {
                    requireActivity().runOnUiThread {
                        binding.btnStartPayment.isEnabled = false
                        when (setupFailure) {
                            is SetupFailure.AlreadyInstalled -> {
                                binding.tvStatus.text = "Near Pay already installed"
                                Toast.makeText(requireContext(), "Near Pay already installed", Toast.LENGTH_SHORT).show()
                            }
                            is SetupFailure.NotInstalled -> {
                                binding.tvStatus.text = "Near Pay installation failed"
                                Toast.makeText(requireContext(), "Near Pay installation failed", Toast.LENGTH_SHORT).show()
                            }
                            is SetupFailure.AuthenticationFailed -> {
                                binding.tvStatus.text = "Authentication failed"
                                Toast.makeText(requireContext(), "Authentication failed", Toast.LENGTH_SHORT).show()
                                nearPay?.updateAuthentication(Jwt("JWT HERE"))
                            }
                            is SetupFailure.InvalidStatus -> {
                                binding.tvStatus.text = "Invalid status: ${setupFailure.status}"
                                Toast.makeText(requireContext(), "Invalid status", Toast.LENGTH_SHORT).show()
                            }

                            SetupFailure.GeneralFailure -> TODO()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            requireActivity().runOnUiThread {
                binding.tvStatus.text = "Setup failed: ${e.message}"
                Toast.makeText(requireContext(), "Setup failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPaymentRequest() {
        if (nearPay == null) {
            Toast.makeText(requireContext(), "Please setup Near Pay first", Toast.LENGTH_SHORT).show()
            return
        }

        val amount: Long = 1455 // Represents 14.55 SAR
        val customerReferenceNumber = "9ace70b7-977d-4094-b7f4-4ecb17de6753"
        val enableReceiptUi = true
        val enableReversal = true
        val finishTimeOut: Long = 10
        val requestId = UUID.randomUUID()
        val enableUiDismiss = true

        binding.tvStatus.text = "Processing payment..."
        binding.btnStartPayment.isEnabled = false

        nearPay?.purchase(
            amount,
            customerReferenceNumber,
            enableReceiptUi,
            enableReversal,
            finishTimeOut,
            requestId,
            enableUiDismiss,
            object : PurchaseListener {
                override fun onPurchaseApproved(transactionData: TransactionData) {
                    requireActivity().runOnUiThread {
                        binding.tvStatus.text = "Payment Approved!"
                        binding.btnStartPayment.isEnabled = true
                        Toast.makeText(requireContext(), "Payment approved successfully!", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onPurchaseFailed(purchaseFailure: PurchaseFailure) {
                    requireActivity().runOnUiThread {
                        binding.btnStartPayment.isEnabled = true
                        when (purchaseFailure) {
                            is PurchaseFailure.PurchaseDeclined -> {
                                binding.tvStatus.text = "Payment Declined"
                                Toast.makeText(requireContext(), "Payment declined", Toast.LENGTH_SHORT).show()
                            }
                            is PurchaseFailure.PurchaseRejected -> {
                                binding.tvStatus.text = "Payment Rejected"
                                Toast.makeText(requireContext(), "Payment rejected", Toast.LENGTH_SHORT).show()
                            }
                            is PurchaseFailure.AuthenticationFailed -> {
                                binding.tvStatus.text = "Authentication Failed"
                                Toast.makeText(requireContext(), "Authentication failed", Toast.LENGTH_SHORT).show()
                                nearPay?.updateAuthentication(Jwt("JWT HERE"))
                            }
                            is PurchaseFailure.InvalidStatus -> {
                                binding.tvStatus.text = "Invalid Status: ${purchaseFailure.status}"
                                Toast.makeText(requireContext(), "Invalid status", Toast.LENGTH_SHORT).show()
                            }
                            is PurchaseFailure.GeneralFailure -> {
                                binding.tvStatus.text = "General Failure"
                                Toast.makeText(requireContext(), "General failure occurred", Toast.LENGTH_SHORT).show()
                            }

                            is PurchaseFailure.UserCancelled -> TODO()
                        }
                    }
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
