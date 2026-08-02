package lk.leoclub.clubprojects.service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lk.leoclub.clubprojects.model.SiteAsset;
import lk.leoclub.clubprojects.repository.SiteAssetRepository;
import lk.leoclub.clubprojects.web.BadRequestException;
import lk.leoclub.clubprojects.web.NotFoundException;

@Service
public class SiteService {

    private static final long MAX_BYTES = 1024L * 1024;

    /**
     * SVG is deliberately not accepted. It can carry script, and serving one
     * from this origin would let it run with the site's privileges. PNG keeps
     * transparency, which is what a logo actually needs.
     */
    private static final List<String> ALLOWED_TYPES =
            List.of("image/png", "image/jpeg", "image/webp");

    private final SiteAssetRepository assets;

    public SiteService(SiteAssetRepository assets) {
        this.assets = assets;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> info() {
        Optional<SiteAsset> logo = assets.findById(SiteAsset.LOGO);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hasLogo", logo.isPresent());
        m.put("logoVersion", logo.map(SiteAsset::getUpdatedAt).orElse(null));
        return m;
    }

    @Transactional(readOnly = true)
    public SiteAsset logo() {
        return assets.findById(SiteAsset.LOGO)
                .orElseThrow(() -> new NotFoundException("No logo has been uploaded."));
    }

    @Transactional
    public Map<String, Object> saveLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose an image to upload.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("That image is larger than 1 MB. Try a smaller one.");
        }

        String type = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT).trim();
        if (!ALLOWED_TYPES.contains(type)) {
            throw new BadRequestException("Use a PNG, JPEG or WebP image for the logo.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("That image could not be read.");
        }
        if (bytes.length == 0) {
            throw new BadRequestException("That image appears to be empty.");
        }

        assets.save(new SiteAsset(SiteAsset.LOGO, bytes, type, System.currentTimeMillis()));
        return info();
    }

    @Transactional
    public void deleteLogo() {
        assets.findById(SiteAsset.LOGO).ifPresent(assets::delete);
    }
}
