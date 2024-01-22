package cc.sovellus.vrcaa.ui.screen.friends

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Badge
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cc.sovellus.vrcaa.api.ApiContext
import cc.sovellus.vrcaa.api.models.Friends
import cc.sovellus.vrcaa.api.utils.StatusUtils
import cc.sovellus.vrcaa.ui.screen.friends.FriendsScreenModel.FriendListState
import cc.sovellus.vrcaa.ui.screen.misc.LoadingIndicatorScreen
import cc.sovellus.vrcaa.ui.screen.profile.FriendProfileScreen
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

class FriendsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current

        val model = navigator.rememberNavigatorScreenModel { FriendsScreenModel(api = ApiContext(context)) }
        val state by model.state.collectAsState()

        when (val result = state) {
            is FriendListState.Loading -> LoadingIndicatorScreen().Content()
            is FriendListState.Result -> RenderList(result.friends, model)
            else -> {}
        }
    }

    @OptIn(
        ExperimentalMaterialApi::class,
        ExperimentalGlideComposeApi::class
    )
    @Composable
    private fun RenderList(friends: List<Friends.FriendsItem>, model: FriendsScreenModel) {

        val navigator = LocalNavigator.currentOrThrow
        val stateRefresh = rememberPullRefreshState(model.isRefreshing.value, onRefresh = { model.refreshFriends() })

        Box(Modifier.pullRefresh(stateRefresh)) {
            if (!model.isRefreshing.value) {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(1.dp)
                ) {
                    // TODO: later add customization to the sort, but for now just put "offline" status to the bottom.
                    val friendsSorted = friends.sortedWith(compareBy { it.location == "offline" })
                    items(friendsSorted.count()) {
                        val friend = friendsSorted[it]
                        ListItem(
                            headlineContent = { Text(friend.statusDescription.ifEmpty { StatusUtils.Status.toString(StatusUtils().getStatusFromString(friend.status)) }, maxLines = 1) },
                            overlineContent = { Text(friend.displayName) },
                            supportingContent = { Text(text = friend.location, maxLines = 1) },
                            leadingContent = {
                                GlideImage(
                                    model = friend.userIcon.ifEmpty { friend.currentAvatarImageUrl },
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(50)),
                                    contentScale = ContentScale.FillBounds,
                                    alignment = Alignment.Center
                                )
                            },
                            trailingContent = {
                                Badge(containerColor = StatusUtils.Status.toColor(StatusUtils().getStatusFromString(friend.status)), modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.clickable(
                                onClick = {
                                    navigator.parent?.parent?.push(FriendProfileScreen(friend))
                                }
                            )
                        )
                    }
                }
            }
            PullRefreshIndicator(model.isRefreshing.value, stateRefresh, Modifier.align(Alignment.TopCenter))
        }
    }
}