(ns clojuredocs.template
  (:require [clojuredocs.context :refer [*db* *router*]]
            [clojure.string :as str]
            [clojuredocs.crypto :as cry]
            [clojuredocs.util :as util]
            [clojuredocs.config :as config]
            [clojuredocs.env :as env]
            [clojuredocs.github :as gh]
            [clojuredocs.search :as search]
            [markdown.core :as md]
            [hiccup.page :refer [html5]]
            [reitit.core :as r]))

(defn gh-auth-url [& [redirect-to-after-auth-url]]
  (let [redirect-url (str "/gh-callback" redirect-to-after-auth-url)]
    (gh/auth-redirect-url
     (merge config/gh-creds
            {:redirect-uri (config/url redirect-url)}))))

(defn $ga-script-tag [ga-tracking-id]
  (when ga-tracking-id
    [:script "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

  ga('create', '" ga-tracking-id "', 'auto');
  ga('send', 'pageview');"]))

(defn $user-area [user]
  [:li.user-area
   [:a {:href "/logout"}
    [:img.avatar {:src (:avatar-url user)}]
    " Log Out"]])

(defn $navbar [{:keys [user hide-search page-uri full-width? show-stars?]}]
  [:header.navbar
   [:div
    {:class (if full-width? "container-fluid" "container")}
    [:div.row
     [:div
      {:class (if full-width?
                "col-md-12"
                "col-md-10 col-md-offset-1")}
      [:a.navbar-brand {:href "/"}
       [:i.fa.fa-rocket]
       "ClojureDocs"]
      #_[:div.navbar-brand.clojure-version
         (:version search/clojure-lib)]
      [:button.btn.btn-default.navbar-btn.pull-right.mobile-menu
       [:i.fa.fa-bars]]
      [:ul.navbar-nav.nav.navbar-right.desktop-navbar-nav
       (when hide-search
         [:li
          [:div.navbar-brand.clojure-version
           [:a {:href (:gh-tag-url search/clojure-lib)}
            (:version search/clojure-lib)]]])
       #_[:li [:a {:href "/jobs"} "Jobs"]]
       [:li [:a {:href "/core-library"} "Core Library"]]
       [:li [:a {:href "/quickref"} "Quick Ref"]]
       (if user
         ($user-area user)
         [:li
          [:a {:href (gh-auth-url page-uri)}
           [:i.fa.fa-github-square] "Log In"]])
       (when show-stars?
         [:li
          [:iframe.gh-starred-count
           {:src "/github-btn.html?user=zk&repo=clojuredocs&type=watch&count=true"
            :allowtransparency "true"
            :frameborder "0"
            :scrolling "0"
            :width "100"
            :height "20"}]])]
      (when-not hide-search
        [:div.nav-search-widget.navbar-right.navbar-form
         [:form.search
          {:autocomplete "off"}
          [:input.placeholder.form-control
           {:type "text"
            :name "query"
            :placeholder "Looking for? (ctrl-s)"
            :autocomplete "off"}]]])]]
    (when-not hide-search
      [:div.row
       [:div.col-md-10.col-md-offset-1
        [:div.ac-results-widget]]])]])

(defn $mobile-navbar-nav [{:keys [user page-uri mobile-nav]}]
  [:div.mobile-nav-menu
   [:section
    [:h4 [:i.fa.fa-rocket] "ClojureDocs"]
    [:ul.navbar-nav.mobile-navbar-nav.nav
     [:li
      [:a {:href "/core-library"} "Core Library"
       [:span.clojure-version "(1.10.1)"]]]
     [:li [:a {:href "/quickref"} "Quick Reference"]]
     (if user
       ($user-area user)
       [:li
        [:a {:href (gh-auth-url page-uri)}
         [:i.fa.fa-github-square] "Log In"]])]]
   (for [{:keys [title links]} mobile-nav]
     [:section
      [:h4 title]
      [:ul.navbar-nav.mobile-navbar-nav.nav
       (for [link links]
         [:li link])]])])

(def clojuredocs-script
  [:script {:src (str "/assets/cljs/clojuredocs.js?"
                      (cry/md5-path "public/cljs/clojuredocs.js"))}])

(def app-link
  [:link {:rel  :stylesheet
          :href (str "/assets/css/app.css?" (cry/md5-path "public/css/app.css"))}])

(def bootstrap-link
  [:link {:rel :stylesheet
          :href (str "/assets/css/bootstrap.min.css?"
                     (cry/md5-path "public/css/bootstrap.min.css"))}])

(def font-awesome-link
  [:link {:rel :stylesheet
          :href (str "/assets/css/font-awesome.min.css?"
                     (cry/md5-path "public/css/font-awesome.min.css"))}])

(def opensearch-link
  [:link {:rel "search"
          :href "/opensearch.xml"
          :type "application/opensearchdescription+xml"
          :title "ClojureDocs"}])

(defn $main [{:keys [page-uri
                     content
                     title
                     body-class
                     user
                     page-data
                     meta
                     full-width?
                     show-survey-banner?] :as opts}]
  [:html
   [:head
    [:meta {:name "viewport" :content "width=device-width, maximum-scale=1.0"}]
    [:meta {:name "apple-mobile-web-app-capable" :content "yes"}]
    [:meta {:name "apple-mobile-web-app-status-bar-style" :content "default"}]
    [:meta {:name "apple-mobile-web-app-title" :content "ClojureDocs"}]
    [:meta {:name "google-site-verification" :content "XjzqkjEPtcgtLjhnqAvtnVSeveEccs-O_unFGGlbk4g"}]
    (->> meta
         (map (fn [[k v]]
                [:meta {:name k :content v}])))
    [:title (or title "Community-Powered Clojure Documentation and Examples | ClojureDocs")]
    opensearch-link
    font-awesome-link
    bootstrap-link
    app-link
    [:script "// <![CDATA[\nwindow.PAGE_DATA=" (util/to-json (pr-str page-data)) ";\n//]]>"]]
   [:body
    (when body-class
      {:class body-class})
    ($mobile-navbar-nav opts)
    [:div.mobile-nav-bar
     ($navbar opts)]
    [:div.sticky-wrapper.mobile-push-wrapper
     (when config/staging-banner?
       [:div.staging-banner
        "This is the ClojureDocs staging site, where you'll find all the neat things we're working on."])
     [:div.desktop-nav-bar
      ($navbar opts)]
     [:div
      {:class (if full-width?
                "container-fluid"
                "container")}
      [:div.row
       [:div
        {:class (if full-width?
                  "col-md-12"
                  "col-md-10 col-md-offset-1")}
        content]]]
     [:div.sticky-push]]
    [:footer
     [:div.container
      [:div.row
       [:div.col-sm-12
        [:div.divider
         "- ❦ -"]]]
      [:div.row
       [:div.ctas
        [:div.col-sm-6.left
         "Brought to you by "
         [:a {:href "https://zacharykim.com"} "Zachary Kim"]
         ". "]
        [:div.col-sm-6.right
         [:iframe.gh-starred-count
          {:src "/github-btn.html?user=zk&repo=clojuredocs&type=watch&count=true"
           :allowtransparency "true"
           :frameborder "0"
           :scrolling "0"
           :width "80"
           :height "20"}]
         [:a.twitter-share-button {:href "https://twitter.com/share"
                                   :data-url "http://clojuredocs.org"
                                   :data-text "Community-powered docs and examples for #Clojure"
                                   :data-via "heyzk"}
          "Tweet"]]]
       [:script
        "!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location)?'http':'https';if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src=p+'://platform.twitter.com/widgets.js';fjs.parentNode.insertBefore(js,fjs);}}(document, 'script', 'twitter-wjs');"]]]]
    (when (env/bool :cljs-dev)
      [:script {:src "/assets/js/fastclick.min.js"}])
    (when (env/bool :cljs-dev)
      [:script {:src "/assets/js/morpheus.min.js"}])
    (when (env/bool :cljs-dev)
      [:script {:src "/assets/js/marked.min.js"}])
    clojuredocs-script
    ($ga-script-tag config/ga-tracking-id)
    ;; mobile safari home screen mode
    [:script
     "if((\"standalone\" in window.navigator) && window.navigator.standalone){
var noddy, remotes = false;

document.addEventListener('click', function(event) {

noddy = event.target;

while(noddy.nodeName !== \"A\" && noddy.nodeName !== \"HTML\") {
noddy = noddy.parentNode;
}

if('href' in noddy && noddy.href.indexOf('http') !== -1 && (noddy.href.indexOf(document.location.host) !== -1 || remotes))
{
event.preventDefault();
document.location.href = noddy.href;
}

},false);
}"]]])

(defn render
  [f]
  (fn [{:as      req
        :keys    [db]
        ::r/keys [router]}]
    (binding [*db*     db
              *router* router]
      (let [resp (f req)]
        (cond
          (nil? resp)    {:status 404 :body "welp"}
          (map? resp)    {:status 200
                          :body   (html5
                                   {:lang :en}
                                   ($main resp))}
          (vector? resp) {:status 200
                          :body   (html5
                                   {:lang :en}
                                   [:head
                                    [:title "awesome"]]
                                   [:body resp])}
          :else          {:status 500 :body "wtf"})))))
