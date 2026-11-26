/*
 * Copyright (c) 2023 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package in.rahulja.materialhn;

import dagger.Component;
import in.rahulja.materialhn.appwidget.WidgetConfigActivity;
import in.rahulja.materialhn.data.ItemSyncAdapter;
import in.rahulja.materialhn.data.ItemSyncJobService;
import in.rahulja.materialhn.data.ItemSyncService;
import in.rahulja.materialhn.widget.MultiPageItemRecyclerViewAdapter;
import in.rahulja.materialhn.widget.SinglePageItemRecyclerViewAdapter;
import in.rahulja.materialhn.widget.StoryRecyclerViewAdapter;
import in.rahulja.materialhn.widget.SubmissionRecyclerViewAdapter;
import javax.inject.Singleton;

@Singleton
@Component(modules = {ApplicationModule.class, ActivityModule.class, UiModule.class})
public interface ApplicationComponent {
  // Application
  void inject(MaterialisticApplication application);

  // Services
  void inject(ItemSyncService itemSyncService);

  void inject(ItemSyncAdapter itemSyncAdapter);

  void inject(in.rahulja.materialhn.appwidget.WidgetHelper widgetHelper);

  void inject(ItemSyncJobService itemSyncJobService);

  // Activities
  void inject(ItemActivity itemActivity);

  void inject(AccountAuthenticatorActivity accountAuthenticatorActivity);

  void inject(ComposeActivity composeActivity);

  void inject(SettingsActivity settingsActivity);

  void inject(FavoriteActivity favoriteActivity);

  void inject(PopularActivity popularActivity);

  void inject(SearchActivity searchActivity);

  void inject(AskActivity askActivity);

  void inject(BestActivity bestActivity);

  void inject(JobsActivity jobsActivity);

  void inject(ListActivity listActivity);

  void inject(NewActivity newActivity);

  void inject(ShowActivity showActivity);

  void inject(FeedbackActivity feedbackActivity);

  void inject(OfflineWebActivity offlineWebActivity);

  void inject(SubmitActivity submitActivity);

  void inject(ThreadPreviewActivity threadPreviewActivity);

  void inject(UserActivity userActivity);

  void inject(WidgetConfigActivity widgetConfigActivity);

  void inject(AboutActivity aboutActivity);

  void inject(ReleaseNotesActivity releaseNotesActivity);

  void inject(LoginActivity loginActivity);

  // Fragments
  void inject(ListFragment listFragment);

  void inject(WebFragment webFragment);

  void inject(ItemFragment itemFragment);

  // Adapters
  void inject(StoryRecyclerViewAdapter storyRecyclerViewAdapter);

  void inject(MultiPageItemRecyclerViewAdapter multiPageItemRecyclerViewAdapter);

  void inject(SinglePageItemRecyclerViewAdapter singlePageItemRecyclerViewAdapter);

  void inject(SubmissionRecyclerViewAdapter submissionRecyclerViewAdapter);

  // Clients
}
