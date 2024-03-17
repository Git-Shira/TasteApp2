package com.example.mypostsapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.mypostsapp.databinding.FragmentCreateProfileBinding
import com.example.mypostsapp.entities.User
import com.example.mypostsapp.ui.main.LoginViewModel
import com.google.firebase.auth.FirebaseAuth

class CreateProfileFragment : Fragment() {

    lateinit var binding: FragmentCreateProfileBinding
    private var  imageBitmap : Bitmap?= null
    private lateinit var loadingDialog: ProgressDialog

    companion object {
        fun newInstance() = CreateProfileFragment()
    }

    private lateinit var viewModel: CreateProfileViewModel

    private val cameraActivityResultLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photo = (result.data!!.extras!!["data"] as Bitmap?)!!
            imageBitmap = photo
            binding.imageView.setImageBitmap(photo)
        }
    }

    private val galleryActivityResultLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val uri = result.data?.data
                val bitmap = MediaStore.Images.Media.getBitmap(this.context?.contentResolver, uri)
                imageBitmap = bitmap
                binding.imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateProfileBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(CreateProfileViewModel::class.java)
        viewModel.onError.observe(viewLifecycleOwner) {
            loadingDialog.dismiss()
            AlertDialogUtils.showAlert(requireContext(), getString(R.string.error), it)
        }

        viewModel.createSuccess.observe(viewLifecycleOwner) {
            loadingDialog.dismiss()
            startActivity(Intent(requireContext(), MainActivity::class.java))
        }
        viewModel.currentUser.observe(viewLifecycleOwner) {
            initializeScreenWithUser(it)
        }
        loadingDialog = ProgressDialog(requireContext())
        loadingDialog.setMessage(getString(R.string.please_wait))


        binding.emailET.setText(FirebaseAuth.getInstance().currentUser?.email.toString())
        binding.fullNameET.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.errorName.visibility = View.GONE;
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })

        binding.addFromGallery.setOnClickListener { openGallery() }
        binding.takePhoto.setOnClickListener { openCamera() }
        binding.save.setOnClickListener {
            loadingDialog.show()
            viewModel.createProfile(FirebaseAuth.getInstance().uid ?: "", binding.fullNameET.text.toString(), imageBitmap)
        }

    }

    private fun initializeScreenWithUser(user: User?) {
        Glide.with(requireContext())
            .load(user?.image)
            .placeholder(R.drawable.baseline_person_24)
            .into(binding.imageView)

        binding.fullNameET.setText(user?.name)
        binding.save.text = getString(R.string.update)
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraActivityResultLauncher.launch(takePictureIntent)
    }

    private fun openGallery() {
        val intent = Intent()
        intent.setType("image/*")
        intent.setAction(Intent.ACTION_GET_CONTENT)
        galleryActivityResultLauncher.launch(intent)
    }
}