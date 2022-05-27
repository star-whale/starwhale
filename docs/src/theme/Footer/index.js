import React from "react";
import { useThemeConfig } from "@docusaurus/theme-common";
import FooterLinks from "@theme/Footer/Links";
import FooterLogo from "@theme/Footer/Logo";
import FooterCopyright from "@theme/Footer/Copyright";
import clsx from "clsx";
import Link from "@docusaurus/Link";
import "./index.scss";
// import FooterLayout from "@theme/Footer/Layout";

function FooterLayout({ style, links, logo, copyright, socials = [] }) {
  return (
    <footer
      className={clsx("footer", {
        "footer--dark": style === "dark",
      })}
    >
      <div className="container container-fluid">
        <div className="logo margin-bottom--sm">
          {logo}
          <div className="social">
            {socials.map((item) => {
              return (
                <Link to={item.to} key={item.icon}>
                  {/* <span className={`iconfont ${item.icon}`}></span> */}
                  <svg className="icon" aria-hidden="true">
                    <use xlinkHref={`#${item.icon}`}></use>
                  </svg>
                </Link>
              );
            })}
          </div>
        </div>
        {links}
      </div>

      {copyright && (
        <div className="footer__bottom text--center">{copyright}</div>
      )}
    </footer>
  );
}

function Footer() {
  const { footer, custom } = useThemeConfig();

  if (!footer) {
    return null;
  }

  const { copyright, links, logo, style } = footer;
  const { footerSocials } = custom;

  return (
    <FooterLayout
      style={style}
      links={links && links.length > 0 && <FooterLinks links={links} />}
      logo={logo && <FooterLogo logo={logo} />}
      socials={footerSocials}
      copyright={copyright && <FooterCopyright copyright={copyright} />}
    />
  );
}

export default React.memo(Footer);
