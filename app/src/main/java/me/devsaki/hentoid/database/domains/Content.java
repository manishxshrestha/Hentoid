package me.devsaki.hentoid.database.domains;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Transient;
import io.objectbox.relation.ToMany;
import me.devsaki.hentoid.activities.websites.ASMHentaiActivity;
import me.devsaki.hentoid.activities.websites.BaseWebActivity;
import me.devsaki.hentoid.activities.websites.EHentaiActivity;
import me.devsaki.hentoid.activities.websites.HentaiCafeActivity;
import me.devsaki.hentoid.activities.websites.HitomiActivity;
import me.devsaki.hentoid.activities.websites.NhentaiActivity;
import me.devsaki.hentoid.activities.websites.PandaActivity;
import me.devsaki.hentoid.activities.websites.PururinActivity;
import me.devsaki.hentoid.activities.websites.TsuminoActivity;
import me.devsaki.hentoid.enums.AttributeType;
import me.devsaki.hentoid.enums.Site;
import me.devsaki.hentoid.enums.StatusContent;
import me.devsaki.hentoid.util.AttributeMap;

/**
 * Created by DevSaki on 09/05/2015.
 * Content builder
 */
@Entity
public class Content implements Serializable {

    @Id
    private long id;
    @Expose
    private String url;
    @Expose(serialize = false, deserialize = false)
    private String uniqueSiteId; // Has to be queryable in DB, hence has to be a field
    @Expose
    private String title;
    @Expose
    private String author;
    @Expose(serialize = false, deserialize = false)
    private ToMany<Attribute> attributes; // TODO - not sure this will result in an n-n relationship. Attributes must be seen as master data - to code app-side ?
    @Expose
    private String coverImageUrl;
    @Expose
    private Integer qtyPages;
    @Expose
    private long uploadDate;
    @Expose
    private long downloadDate;
    @Expose
    @Convert(converter = StatusContent.StatusContentConverter.class, dbType = Integer.class)
    private StatusContent status;
    @Expose(serialize = false, deserialize = false)
    @Backlink(to = "content")
    private ToMany<ImageFile> imageFiles;
    @Expose
    @Convert(converter = Site.SiteConverter.class, dbType = Long.class)
    private Site site;
    private String storageFolder; // Not exposed because it will vary according to book location -> valued at import
    @Expose
    private boolean favourite;
    @Expose
    private long reads = 0;
    @Expose
    private long lastReadDate;
    // Runtime attributes; no need to expose them nor to persist them
    @Transient
    private double percent;
    @Transient
    private int queryOrder;
    @Transient
    private boolean selected = false;
    // Kept for retro-compatibility with contentV2.json Hentoid files
    @Transient
    @Expose
    @SerializedName("attributes")
    private AttributeMap attributeMap;
    @Transient
    @Expose
    @SerializedName("imageFiles")
    private ArrayList<ImageFile> imageList;


    public List<Attribute> getAttributes() {
        return this.attributes;
    }

    public void setAttributes(ToMany<Attribute> attributes) {
        this.attributes = attributes;
    }

    public AttributeMap getAttributeMap() {
        AttributeMap result = new AttributeMap();
        result.add(attributes);
        return result;
    }

    public Content addAttributes(AttributeMap attributes) {
        if (attributes != null) {
            for (AttributeType type : attributes.keySet()) {
                this.attributes.addAll(attributes.get(type));
            }
        }
        return this;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUniqueSiteId() {
        return this.uniqueSiteId;
    }

    private String computeUniqueSiteId() {
        String[] paths;

        switch (site) {
            case FAKKU:
                return url.substring(url.lastIndexOf("/") + 1);
            case EHENTAI:
            case PURURIN:
                paths = url.split("/");
                return paths[1];
            case HITOMI:
                paths = url.split("/");
                return paths[1].replace(".html", "");
            case ASMHENTAI:
            case ASMHENTAI_COMICS:
            case NHENTAI:
            case PANDA:
            case TSUMINO:
                return url.replace("/", "");
            case HENTAICAFE:
                return url.replace("/?p=", "");
            default:
                return "";
        }
    }

    // Used for upgrade purposes
    @Deprecated
    public String getOldUniqueSiteId() {
        String[] paths;
        switch (site) {
            case FAKKU:
                return url.substring(url.lastIndexOf("/") + 1);
            case PURURIN:
                paths = url.split("/");
                return paths[2].replace(".html", "") + "-" + paths[1];
            case HITOMI:
                paths = url.split("/");
                return paths[1].replace(".html", "") + "-" +
                        title.replaceAll("[^a-zA-Z0-9.-]", "_");
            case ASMHENTAI:
            case ASMHENTAI_COMICS:
            case NHENTAI:
            case PANDA:
            case EHENTAI:
            case TSUMINO:
                return url.replace("/", "") + "-" + site.getDescription();
            case HENTAICAFE:
                return url.replace("/?p=", "") + "-" + site.getDescription();
            default:
                return null;
        }
    }

    public Class<?> getWebActivityClass() {
        switch (site) {
            case HITOMI:
                return HitomiActivity.class;
            case NHENTAI:
                return NhentaiActivity.class;
            case ASMHENTAI:
            case ASMHENTAI_COMICS:
                return ASMHentaiActivity.class;
            case HENTAICAFE:
                return HentaiCafeActivity.class;
            case TSUMINO:
                return TsuminoActivity.class;
            case PURURIN:
                return PururinActivity.class;
            case EHENTAI:
                return EHentaiActivity.class;
            case PANDA:
                return PandaActivity.class;
            default:
                return BaseWebActivity.class; // Fallback for FAKKU
        }
    }

    public String getCategory() {
        if (site == Site.FAKKU) {
            return url.substring(1, url.lastIndexOf("/"));
        } else {
            if (attributes != null) {
                List<Attribute> attributesList = getAttributeMap().get(AttributeType.CATEGORY);
                if (attributesList != null && attributesList.size() > 0) {
                    return attributesList.get(0).getName();
                }
            }
        }

        return null;
    }

    public String getUrl() {
        return url;
    }

    public Content setUrl(String url) {
        this.url = url;
        this.uniqueSiteId = computeUniqueSiteId();
        return this;
    }

    public String getGalleryUrl() {
        String galleryConst;
        switch (site) {
            case PURURIN:
                galleryConst = "/gallery";
                break;
            case HITOMI:
                galleryConst = "/galleries";
                break;
            case ASMHENTAI:
            case ASMHENTAI_COMICS:
            case EHENTAI:           // Won't work because of the temporary key
            case NHENTAI:
                galleryConst = "/g";
                break;
            case TSUMINO:
                galleryConst = "/Book/Info";
                break;
            case HENTAICAFE:
            case PANDA:
            default:
                galleryConst = "";
                break; // Includes FAKKU & Hentai Cafe
        }

        return site.getUrl() + galleryConst + url;
    }

    public String getReaderUrl() {
        switch (site) {
            case HITOMI:
                return site.getUrl() + "/reader" + url;
//            case NHENTAI:
//                return getGalleryUrl() + "1/";
            case TSUMINO:
                return site.getUrl() + "/Read/View" + url;
            case ASMHENTAI:
                return site.getUrl() + "/gallery" + url + "1/";
            case ASMHENTAI_COMICS:
                return site.getUrl() + "/gallery" + url;
            case EHENTAI:               // Won't work anyway because of the temporary key
            case HENTAICAFE:
            case NHENTAI:
            case PANDA:
                return getGalleryUrl();
            case PURURIN:
                return site.getUrl() + "/read/" + url.substring(1).replace("/", "/01/");
            default:
                return null;
        }
    }

    public Content populateAuthor() {
        String author = "";
        AttributeMap attrMap = getAttributeMap();
        if (attrMap.containsKey(AttributeType.ARTIST) && attrMap.get(AttributeType.ARTIST).size() > 0)
            author = attrMap.get(AttributeType.ARTIST).get(0).getName();
        if (null == author || author.equals("")) // Try and get Circle
        {
            if (attrMap.containsKey(AttributeType.CIRCLE) && attrMap.get(AttributeType.CIRCLE).size() > 0)
                author = attrMap.get(AttributeType.CIRCLE).get(0).getName();
        }
        if (null == author) author = "";
        setAuthor(author);
        return this;
    }

    public Content preJSONExport() { // TODO - this is shabby
        this.attributeMap = getAttributeMap();
        this.imageList = new ArrayList<>(imageFiles);
        return this;
    }

    public Content postJSONImport() {   // TODO - this is shabby
        if (this.attributeMap != null) {
            this.attributes.clear();
            for (AttributeType type : this.attributeMap.keySet())
                this.attributes.addAll(this.attributeMap.get(type));
        }
        if (this.imageList != null) {
            this.imageFiles.clear();
            this.imageFiles.addAll(this.imageList);
        }
        this.uniqueSiteId = computeUniqueSiteId();
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Content setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getAuthor() {
        if (null == author) populateAuthor();
        return author;
    }

    public Content setAuthor(String author) {
        this.author = author;
        return this;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public Content setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
        return this;
    }

    public Integer getQtyPages() {
        return qtyPages;
    }

    public Content setQtyPages(Integer qtyPages) {
        this.qtyPages = qtyPages;
        return this;
    }

    public long getUploadDate() {
        return uploadDate;
    }

    public Content setUploadDate(long uploadDate) {
        this.uploadDate = uploadDate;
        return this;
    }

    public long getDownloadDate() {
        return downloadDate;
    }

    public Content setDownloadDate(long downloadDate) {
        this.downloadDate = downloadDate;
        return this;
    }

    public StatusContent getStatus() {
        return status;
    }

    public Content setStatus(StatusContent status) {
        this.status = status;
        return this;
    }

    public ToMany<ImageFile> getImageFiles() {
        return imageFiles;
    }

    public Content addImageFiles(List<ImageFile> imageFiles) {
        this.imageFiles.clear();
        this.imageFiles.addAll(imageFiles);
        return this;
    }

    public double getPercent() {
        return percent;
    }

    public Content setPercent(double percent) {
        this.percent = percent;
        return this;
    }

    public Site getSite() {
        return site;
    }

    public Content setSite(Site site) {
        this.site = site;
        return this;
    }

    public String getStorageFolder() {
        return storageFolder == null ? "" : storageFolder;
    }

    public Content setStorageFolder(String storageFolder) {
        this.storageFolder = storageFolder;
        return this;
    }

    public boolean isFavourite() {
        return favourite;
    }

    public Content setFavourite(boolean favourite) {
        this.favourite = favourite;
        return this;
    }

    private int getQueryOrder() {
        return queryOrder;
    }

    public Content setQueryOrder(int order) {
        queryOrder = order;
        return this;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }


    public long getReads() {
        return reads;
    }

    public Content increaseReads() {
        this.reads++;
        return this;
    }

    public Content setReads(long reads) {
        this.reads = reads;
        return this;
    }

    public long getLastReadDate() {
        return (0 == lastReadDate) ? downloadDate : lastReadDate;
    }

    public Content setLastReadDate(long lastReadDate) {
        this.lastReadDate = lastReadDate;
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Content content = (Content) o;

        return (url != null ? url.equals(content.url) : content.url == null) && site == content.site;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (site != null ? site.hashCode() : 0);
        return result;
    }

    public static final Comparator<Content> TITLE_ALPHA_COMPARATOR = (a, b) -> a.getTitle().compareTo(b.getTitle());

    public static final Comparator<Content> DLDATE_COMPARATOR = (a, b) -> Long.compare(a.getDownloadDate(), b.getDownloadDate()) * -1; // Inverted - last download date first

    public static final Comparator<Content> ULDATE_COMPARATOR = (a, b) -> Long.compare(a.getUploadDate(), b.getUploadDate()) * -1; // Inverted - last upload date first

    public static final Comparator<Content> TITLE_ALPHA_INV_COMPARATOR = (a, b) -> a.getTitle().compareTo(b.getTitle()) * -1;

    public static final Comparator<Content> DLDATE_INV_COMPARATOR = (a, b) -> Long.compare(a.getDownloadDate(), b.getDownloadDate());

    public static final Comparator<Content> READS_ORDER_COMPARATOR = (a, b) -> {
        int comp = Long.compare(a.getReads(), b.getReads());
        return (0 == comp) ? Long.compare(a.getLastReadDate(), b.getLastReadDate()) : comp;
    };

    public static final Comparator<Content> READS_ORDER_INV_COMPARATOR = (a, b) -> {
        int comp = Long.compare(a.getReads(), b.getReads()) * -1;
        return (0 == comp) ? Long.compare(a.getLastReadDate(), b.getLastReadDate()) * -1 : comp;
    };

    public static final Comparator<Content> READ_DATE_INV_COMPARATOR = (a, b) -> Long.compare(a.getLastReadDate(), b.getLastReadDate()) * -1;

    public static final Comparator<Content> QUERY_ORDER_COMPARATOR = (a, b) -> Integer.compare(a.getQueryOrder(), b.getQueryOrder());
}
