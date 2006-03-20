package net.databinder.components;

import org.hibernate.Query;

import net.databinder.models.IQueryBinder;
import wicket.ajax.AjaxRequestTarget;
import wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import wicket.ajax.markup.html.AjaxLink;
import wicket.markup.html.PackageResourceReference;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.TextField;
import wicket.markup.html.image.Image;
import wicket.markup.html.panel.Panel;
import wicket.model.IModel;

/**
 * Panel for a "live" search field with a clear button. The SearchPanel's model object
 * is linked to the text of the search field inside it. Instances of this class must
 * implement the onUpdate method to register external components for updating.
 * @author Nathan Hamblen
 */
public abstract class SearchPanel extends Panel {
	
	/**
	 * @param id Wicket id
	 * @param searchModel model to receive search string
	 */
	public SearchPanel(String id, IModel searchModel) {
		super(id, searchModel);
		add(new SearchForm("searchForm", searchModel));
		add(new StyleLink("searchStylesheet", SearchPanel.class));
	}

	
	/**
	 * Override to add components to be updated (or JavaScript to be executed)
	 * when the search string changes. Remember that added components must
	 * have a markup id; use component.setMarkupId(true) to assign one 
	 * programmatically.
	 * @param target Ajax target to register components for update
	 */
	public abstract void onUpdate(AjaxRequestTarget target);
	
	/**
	 * Binds the search model to a "search" parameter in a query. The value in the 
	 * search field will be bracketed by percent signs (%) for a find-anywhere match.
	 * In the query itself, "search" must be the name of the  one and only parameter 
	 * If your needs differ, bind the model passed in to the SearchPanel constructor 
	 * to your own IQueryBinder instance; this is a convenience method. 
	 * @return binder for a "search" parameter
	 */
	public IQueryBinder getQueryBinder() {
		return new IQueryBinder() {
			public void bind(Query query) {
				query.setString("search", getModelObject() == null ? 
						null : "%" + getModelObject() + "%");
			}
		};
	}
	
	/** Form with AJAX components and their wrappers. */
	public class SearchForm extends Form {
		public SearchForm(String id, IModel searchModel) {
			super(id);

			final WebMarkupContainer searchWrap = new WebMarkupContainer("searchWrap");
			add(searchWrap.setOutputMarkupId(true));
			final TextField search = new TextField("search", searchModel);
			search.setOutputMarkupId(true);
			searchWrap.add(search);

			final WebMarkupContainer clearWrap = new WebMarkupContainer("clearWrap");
			add(clearWrap.setOutputMarkupId(true));
			final AjaxLink clearLink = new AjaxLink("clearLink") {
				/** Clear field and register updates. */
				public void onClick(AjaxRequestTarget target) {
					search.setModelObject(null);
					target.addComponent(searchWrap);
					target.addComponent(clearWrap);
					SearchPanel.this.onUpdate(target);
				}
				/** Hide when search is blank. */
				public boolean isVisible() {
					return search.getModelObject() != null;
				}
			};
			clearLink.setOutputMarkupId(true);
			clearLink.add( new Image("clear", 
					new PackageResourceReference(this.getClass(), "clear.png")));
			clearWrap.add(clearLink);

			// listen on key up events
			search.add(new AjaxFormComponentUpdatingBehavior("onkeyup") {
				protected void onUpdate(AjaxRequestTarget target) {
					target.addComponent(clearWrap);
					SearchPanel.this.onUpdate(target);
				}
			});
		}
	}
}