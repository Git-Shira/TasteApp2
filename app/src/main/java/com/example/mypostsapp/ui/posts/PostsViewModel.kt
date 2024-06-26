package com.example.mypostsapp.ui.posts

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mypostsapp.DataBaseManager
import com.example.mypostsapp.entities.Post
import com.example.mypostsapp.entities.User
import com.example.mypostsapp.room.RoomManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostsViewModel : ViewModel() {

    private val allPosts = ArrayList<Post>()
    private val myPostsOnly = ArrayList<Post>()
    private val likedOnly = ArrayList<Post>()
    private var relevantPosts = ArrayList<Post>()
    private var type: PostsScreenType = PostsScreenType.ALL

    val postsLD: MutableLiveData<ArrayList<Post>> = MutableLiveData(allPosts)

    private var user : User ? = null
    val userLD: MutableLiveData<User?> = MutableLiveData()

    fun addPost(post: Post, user: User) {
        allPosts.add(0, post)
        if (relevantPosts != allPosts) {
            relevantPosts.add(0, post)
        }
        this.user = user
        postsLD.postValue(postsLD.value)
    }

    fun updatePost(position: Int, post: Post, user: User) {
        relevantPosts[position] = post
        if (relevantPosts != allPosts) {
           allPosts.forEachIndexed { index, currentPost ->
                if (post.uid == currentPost.uid) {
                    allPosts[index] = post
                }
            }

        }
        this.user = user
        publishPostsList(relevantPosts, false)
    }

    private fun publishPostsList(postToPublish: ArrayList<Post>, clearTable: Boolean = true) {
        postToPublish.sortByDescending { it.created }
        relevantPosts = postToPublish
        postsLD.postValue(relevantPosts)
        viewModelScope.launch(Dispatchers.IO) {
            if (clearTable) {
                RoomManager.database.postDao().clearPostsTable()
            }
            postsLD.value?.forEach {
                RoomManager.database.postDao().insertPost(it)
            }
        }
    }

    fun onLikeClicked(position: Int) {
        val selectedPost = relevantPosts[position]
        selectedPost.likeUserIds?.firstOrNull {
            userLD.value?.uid == it
        }?.let {
            selectedPost.likeUserIds?.remove(it)
            selectedPost.likeUserIds?.remove(it)
            postsLD.postValue(relevantPosts)
        } ?: run {
            selectedPost.likeUserIds?.add(userLD.value?.uid!!) ?: run {
                selectedPost.likeUserIds = arrayListOf()
                selectedPost.likeUserIds?.add(userLD.value?.uid!!)
            }
            postsLD.postValue(relevantPosts)
        }

        DataBaseManager.savePost(selectedPost)
    }

    fun onDeleteClicked(position: Int) {
        val selectedPost = relevantPosts[position]
        allPosts.remove(selectedPost)
        relevantPosts.remove(selectedPost)
        postsLD.postValue(relevantPosts)
        DataBaseManager.deletePost(selectedPost, userLD.value)
        viewModelScope.launch(Dispatchers.IO) {
            RoomManager.database.postDao().deletePost(selectedPost)
            userLD.value?.let {
                RoomManager.database.userDao().insertUser(it)
            }
        }
    }

    fun setPostsType(postsScreenType: PostsScreenType?) {
        this.type = postsScreenType ?: PostsScreenType.ALL
    }

    fun fetchPosts(loadFromRoom: Boolean = true) {
        DataBaseManager.fetchPosts(viewModelScope, loadFromRoom, allPosts) {
            when (type) {
                PostsScreenType.ALL -> {
                    publishPostsList(allPosts)
                }

                PostsScreenType.MY_POSTS -> {
                    myPostsOnly.clear()
                    myPostsOnly.addAll(allPosts.filter { it.createdUser?.uid == FirebaseAuth.getInstance().uid })
                    publishPostsList(myPostsOnly)
                }
                PostsScreenType.LIKED -> {
                    likedOnly.clear()
                    likedOnly.addAll(allPosts.filter { it.likeUserIds?.contains(FirebaseAuth.getInstance().uid) == true })
                    publishPostsList(likedOnly)
                }
            }
        }

        DataBaseManager.getCurrentUser(FirebaseAuth.getInstance().uid!!) { it ->
            if (it.isSuccessful) {
                user = it.result.toObject(User::class.java)
                userLD.postValue(user)
                viewModelScope.launch(Dispatchers.IO) {
                    user?.let { user->
                        RoomManager.database.userDao().insertUser(user)
                    }
                }
            }
        }
    }


}