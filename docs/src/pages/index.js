/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

import React from "react";
import clsx from "clsx";
import Layout from "@theme/Layout";
import Link from "@docusaurus/Link";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import useBaseUrl from "@docusaurus/useBaseUrl";
import styles from "./styles.module.css";
import "./index.scss";


function Feature({ imageUrl, title, description, newline }) {
    const imgUrl = useBaseUrl(imageUrl);
    return (
        <div
            className={clsx(
                `col col--4 col--feature text--justified  ${newline}`,
                styles.feature
            )}
        >
            {imgUrl && (
                <div>
                    <img className={styles.featureImage} src={imgUrl} alt={title} />
                </div>
            )}
            <h3>{title}</h3>
            <p>{description}</p>
        </div>
    );
}

function Home() {
    const context = useDocusaurusContext();
    const { siteConfig = {} } = context;
    return (
        <Layout
            title={`Docs`}
            description="Description will go into a meta tag in <head />"
        >
            <div className="swMain-wrapper">
                <header className={clsx("hero hero--primary", styles.heroBanner)}>
                    <div className="container">
                        <h1 className={styles.heroProjectTagline}>
                            <span
                                className={styles.heroTitleTextHtml}
                                dangerouslySetInnerHTML={{
                                    __html: siteConfig.tagline,
                                }}
                            />
                        </h1>
                        <div className={styles.indexCtas}>
                            <Link
                                className="button button--outline button--secondary button--lg button--rounded"
                                to="/docs/quickstart/standalone"
                            >
                                Get Started
                            </Link>
                            <span className={styles.indexCtasGitHubButtonWrapper}>
                                <iframe
                                    className={styles.indexCtasGitHubButton}
                                    src="https://ghbtns.com/github-btn.html?user=star-whale&amp;repo=starwhale&amp;type=star&amp;count=true&amp;size=large"
                                    width={160}
                                    height={30}
                                    title="GitHub Stars"
                                />
                            </span>
                        </div>
                    </div>
                </header>
            </div>
        </Layout>
    );
}

export default Home;
