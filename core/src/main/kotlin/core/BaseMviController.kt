package ru.appkode.base.ui.core.core

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hannesdorfmann.mosby3.MviController
import com.hannesdorfmann.mosby3.mvi.MviPresenter
import com.jakewharton.rxrelay2.PublishRelay
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.*

abstract class BaseMviController<VS, V : MviView<VS>, P : MviPresenter<V, VS>>
  : MviController<V, P>, MviView<VS>,
  LayoutContainer {

  /**
   * A base interface for configuring BaseMviController.
   * Abstract subclasses may wish to extend this config interface by adding some fields to it
   */
  interface Config {
    val viewLayoutResource: Int
    val clearPreviousStateOnDestroy: Boolean
      get() = true
  }

  /**
   * Contains a cached copy of configuration created by [createConfig] during controller
   * construction
   */
  protected val config: Config by lazy(LazyThreadSafetyMode.NONE) { createConfig() }
  protected val eventsRelay: PublishRelay<Pair<Int, Any>> = PublishRelay.create()
  protected var previousViewState: VS? = null

  private var bindPropsRootView: View? = null

  protected abstract fun createConfig(): Config
  override val containerView: View? get() = bindPropsRootView

  constructor()
  constructor(args: Bundle) : super(args)

  final override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    val rootView = inflater.inflate(config.viewLayoutResource, container, false)
    bindPropsRootView = rootView
    // initialization can happen only after bindPropsRootView is assigned,
    // this is required for BindView delegate to work
    initializeView(rootView)
    return rootView
  }

  abstract fun initializeView(rootView: View)

  override fun onDestroyView(view: View) {
    clearFindViewByIdCache()
    bindPropsRootView = null
    super.onDestroyView(view)
    if (config.clearPreviousStateOnDestroy) {
      previousViewState = null
    }
  }

  final override fun render(viewState: VS) {
    renderViewState(viewState)
    previousViewState = viewState
  }

  protected fun fieldChanged(newState: VS, field: (VS) -> Any): Boolean {
    return if (previousViewState == null) true
    else (field.invoke(previousViewState!!) != field.invoke(newState))
  }

  protected abstract fun renderViewState(viewState: VS)
}
